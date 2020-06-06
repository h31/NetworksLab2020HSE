from flask import render_template, redirect, request

from app.crawler import Crawler
from app import app


@app.route('/', methods=['GET'])
def crawler():
    return render_template('crawler.html')


@app.route('/crawl', methods=['POST'])
def add_url():
    url = request.form['url']
    print(f'url={url}')
    try:
        Crawler().process(url)
    except Exception as e:
        print(f"Fail {e}")

    return redirect('/')
