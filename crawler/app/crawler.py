import hashlib
import os
from urllib.parse import urljoin
from bs4 import BeautifulSoup
from numpy.core import unicode
import requests


class Crawler(object):

    @staticmethod
    def download_resources(tag, attribute, url, soup, dir):
        for resource in soup.findAll(tag):
            try:
                res_url = resource[attribute]
                res_response = requests.get(urljoin(url, res_url))
                res_filename = os.path.join(dir, hashlib.sha256(res_url.encode('utf-8')).hexdigest())
                with open(res_filename, "wb") as output:
                    output.write(res_response.content)
                    resource[attribute] = res_filename
            except KeyError:
                print('[EROOR] Can not find attribute %s' % attribute)
            except OSError as e:
                print('[EROOR] Can not load resource by url %s' % url)

    def download_all_resources(self, url, soup, dir):
        self.download_resources('video', 'src', url, soup, dir)
        self.download_resources('audio', 'src', url, soup, dir)
        self.download_resources('img', 'src', url, soup, dir)

    @staticmethod
    def download_html_page(content, dir):
        filename = os.path.join(dir, 'page.html')
        with open(filename, "wb") as output:
            output.write(content)

    def process(self, url):
        response = requests.get(url)
        url = response.url
        if response.headers['Content-Type'].find('text/html') == -1:
            raise Exception("[EROOR] Not interested in files of type %s" % response.headers['Content-Type'])
        content = unicode(response.content, response.encoding, errors="replace")
        soup = BeautifulSoup(content, 'html.parser')

        url_hash = hashlib.sha256(url.encode('utf-8')).hexdigest()
        dir = os.path.join('app', 'templates', hashlib.sha256(url.encode('utf-8')).hexdigest())
        os.mkdir(dir)

        self.download_html_page(response.content, dir)
        self.download_all_resources(url, soup, dir)
        self.save_url(url, url_hash)

    @staticmethod
    def save_url(url, url_hash):
        with open(os.path.join('app', 'templates', 'crawler.html'), 'r') as urls_html:
            urls = urls_html.read()
            soup = BeautifulSoup(urls, 'html.parser')
            href = os.path.join(url_hash, 'page.html')
            url_tag = soup.new_tag('a', href=href)
            url_tag.string = url
            soup.body.append(url_tag)
            with open(os.path.join('app', 'templates', 'crawler.html'), 'w') as urls_html:
                urls_html.write(str(soup))
