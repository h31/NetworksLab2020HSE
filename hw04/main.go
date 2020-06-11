package main

import (
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"regexp"
)

func addEntry(title string) {
	file, _ := os.OpenFile("list.html", os.O_APPEND|os.O_WRONLY, 0600)
	file.WriteString(`<p><a href="/` + title + `">/` + title + "</a></p>\n")
	defer file.Close()
}

func newHandler(w http.ResponseWriter, r *http.Request, title string)  {
	_, err := os.Open("files/" + title + "html")
	if err == nil {
		http.Redirect(w, r, "/"+title, http.StatusFound)
		return
	}
	err = crawlPage("http://" + title, 2)
	if err != nil {
		http.NotFound(w, r)
		return
	}
	addEntry(title)
	http.Redirect(w, r, "/"+title, http.StatusFound)
}

func mainHandler(w http.ResponseWriter, r *http.Request, title string) {
	var file *os.File
	var err error
	if title == "list" {
		file, err = os.Open("list.html")
	} else {
		file, err = os.Open("files/" + title + ".html")
	}
	if err != nil {
		http.NotFound(w, r)
	}
	b, _ :=ioutil.ReadAll(file)
	fmt.Fprintf(w, string(b))
}

func originalHandler(fn func(http.ResponseWriter, *http.Request, string)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		m := regexp.MustCompile("^/(.+)$").FindStringSubmatch(r.URL.Path)
		if m == nil {
			http.NotFound(w, r)
			return
		}
		fn(w, r, m[1])
	}
}

func createHandler(fn func(http.ResponseWriter, *http.Request, string)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		m := regexp.MustCompile("^/new/(.+)$").FindStringSubmatch(r.URL.Path)
		if m == nil {
			http.NotFound(w, r)
			return
		}
		fn(w, r, m[1])
	}
}

func main() {
	os.Mkdir("files", 0755)
	http.HandleFunc("/", originalHandler(mainHandler))
	http.HandleFunc("/new/", createHandler(newHandler))

	log.Fatal(http.ListenAndServe(":8080", nil))
}