package main

import (
	"bytes"
	"fmt"
	"github.com/PuerkitoBio/goquery"
	"io"
	"io/ioutil"
	"log"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
)

func crawlPage(urlStr string, depth int) error {
	resp, err := http.Get(urlStr)
	if err != nil {
		return err
	}

	defer resp.Body.Close()
	bodyBytes, _ := ioutil.ReadAll(resp.Body)
	resp.Body = ioutil.NopCloser(bytes.NewBuffer(bodyBytes))

	uurl, err := url.Parse(urlStr)

	filename := saveURL(uurl.Host + uurl.Path, *resp)
	_ = saveURL(uurl.Host + uurl.Path + ".html", *resp)
	//downloadImages(bytes.NewBuffer(bodyBytes), url)
	if depth > 0 {
		downloadAllLinks(bytes.NewBuffer(bodyBytes), depth - 1, urlStr)
		fixAllLinks(bytes.NewBuffer(bodyBytes), filename, urlStr)
	}


	return nil
}

func fixAllLinks(buf *bytes.Buffer, filename string, startURL string) {
	document, err := goquery.NewDocumentFromReader(buf)
	if err != nil {
		log.Fatal("Error loading HTTP response body. ", err)
	}

	document.Find("a").Each(func (index int, element *goquery.Selection) {
		href, exists := element.Attr("href")
		if exists {
			fmt.Println(href)
			var uurl *url.URL
			//element.SetText(uurl.Host + uurl.Path)
			if href[0] == '/' {
				uurl, _ = url.Parse(startURL + href)
			} else {
				uurl, _ = url.Parse(href)
			}
			element.SetAttr("href", "/" + uurl.Host + uurl.Path)
		}
	})

	//file, _ := os.OpenFile(filename, os.O_WRONLY, 0644)
	//defer file.Close()
	os.Remove(filename)
	os.MkdirAll(filepath.Dir(filename), 0755)
	file, err := os.Create(filename)
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()

	html, _ := goquery.OuterHtml(document.Selection)

	//_, err = io.Copy(file, bytes.NewBufferString(html))
	_, err = io.Copy(file, bytes.NewBuffer([]byte(html)))
	if err != nil {
		log.Fatal(err)
	}

}



func downloadAllLinks(buf *bytes.Buffer, depth int, startURL string) {
	document, err := goquery.NewDocumentFromReader(buf)
	if err != nil {
		log.Fatal("Error loading HTTP response body. ", err)
	}

	document.Find("a").Each(func (index int, element *goquery.Selection) {
		href, exists := element.Attr("href")
		if exists {
			fmt.Println(href)
			if href[0] == '/' {
				go crawlPage(startURL + href, depth)
			} else {
				go crawlPage(href, depth)
			}
		}
	})
}

// training function, remove or fix before push
func downloadImages(buf *bytes.Buffer, url string) error {
	document, err := goquery.NewDocumentFromReader(buf)
	if err != nil {
		log.Fatal("Error loading HTTP response body. ", err)
	}

	document.Find("img").Each(func(index int, element *goquery.Selection) {
		imgSrc, exists := element.Attr("src")
		if exists {
			fmt.Println(imgSrc)
			respImg, _ := http.Get(url + imgSrc)
			saveURL("img1.png", *respImg);
		}
		element.SetText("img1.png")
	})

	return nil
}

func saveURL(title string, resp http.Response) string {
	var filename string
	if title[len(title) - 1] == '/' {
		filename = title[:(len(title) - 1)]
	} else {
		filename = title
	}
	filename = "files/" + filename + ".html"

	os.MkdirAll(filepath.Dir(filename), 0755)
	file, err := os.Create(filename)
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()

	_, err = io.Copy(file, resp.Body)
	if err != nil {
		log.Fatal(err)
	}
	return filename
}
