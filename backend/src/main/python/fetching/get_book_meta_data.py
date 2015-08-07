#!/usr/bin/env python

import sys
import urllib2
from semantics3 import Products

def getBookFromGoogleAPI(isbn):
  response = urllib2.urlopen("https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn)
  html = response.read()
  return html

def getBookFromSematics3(isbn, product):
  product.products_field("cat_id", 12597)
  product.products_field("isbn", isbn)
  results = product.get_products()
  return results
if __name__ == '__main__':
  if not sys.argv[1]:
    print "Please enter isbn of the book you are looking for"
    exit(1)
  isbn = sys.argv[1]
  products = Products(api_key = sys.argv[2], api_secret = sys.argv[3])

  print "Google Result"
  result = getBookFromGoogleAPI(isbn)
  print result

  print "Sematics3 Result"
  result = getBookFromSematics3(isbn, products)
  print result
