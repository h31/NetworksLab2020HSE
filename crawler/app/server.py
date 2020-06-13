import os

from flask import render_template, redirect, request, Flask

from app.crawler import Crawler, Href

app = Flask(__name__)

hrefs = []


@app.route('/', methods=['GET'])
def main():
    return render_template('crawler.html', hrefs=hrefs)


@app.route('/crawl', methods=['POST'])
def crawl():
    url = request.form['url']
    print(f'url={url}')
    try:
       hrefs.append(Crawler().process(url))
    except Exception as e:
        print(f"Fail {e}")

    return redirect('/')


@app.route('/<hash>', methods=['GET'])
def open_page(hash):
    try:
        with open(os.path.join('templates', 'pages',  hash), 'r', encoding='utf-8') as input:
            return input.read()
    except Exception as e:
        print(f"Fail {e}")
        return redirect('/')


if __name__ == '__main__':
    with open('urls', 'r') as input:
        for line in input.readlines():
            url, hash = line.split()
            hrefs.append(Href(url, hash))
    app.run(debug=True)
