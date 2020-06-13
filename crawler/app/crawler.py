import hashlib
import os
from urllib.parse import urljoin
from bs4 import BeautifulSoup
from numpy.core import unicode
import requests


class Href:
    def __init__(self, url, hash):
        self.url = url
        self.hash = hash


class Crawler(object):

    @staticmethod
    def download_resources(tag, attribute, soup, href):
        for resource in soup.findAll(tag):
            try:
                res_url = resource[attribute]
                res_response = requests.get(urljoin(href.url, res_url))
                res_hash = hashlib.sha256(res_url.encode('utf-8')).hexdigest()
                res_file = os.path.join('templates', 'pages', 'images', res_hash)
                with open(res_file, "wb") as output:
                    output.write(res_response.content)
                    resource[attribute] = f'../images/{res_hash}'
                    resource['href'] = res_hash
            except KeyError:
                print('[EROOR] Can not find attribute %s' % attribute)
            except OSError:
                print('[EROOR] Can not load resource by url %s' % href.url)

    def download_all_resources(self, soup, href):
        self.download_resources('video', 'src', soup, href)
        self.download_resources('audio', 'src', soup, href)
        self.download_resources('img', 'src', soup, href)

    def download_html_page(self, soup, href):
        self.download_all_resources(soup, href)
        with open(os.path.join('templates', 'pages', href.hash), "w", encoding='utf-8') as output:
            output.write(str(soup))

    def process(self, url):
        response = requests.get(url)
        url = response.url
        if response.headers['Content-Type'].find('text/html') == -1:
            raise Exception("[EROOR] Not interested in files of type %s" % response.headers['Content-Type'])
        content = unicode(response.content, response.encoding, errors="replace")
        soup = BeautifulSoup(content, 'html.parser')

        href = Href(url, hashlib.sha256(url.encode('utf-8')).hexdigest())

        self.download_html_page(soup, href)
        self.save(href)
        return href

    @staticmethod
    def save(href):
        with open('urls', 'w') as urls:
            urls.write(f'{href.url} {href.hash}')
