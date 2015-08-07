require 'sjsu_class_fetch'
require 'sjsu_class_parser'

fetch = SJSUClassFetch.new
class_number = '49083'
page = fetch.get(class_number)
parser = SJSUClassParser.new(page)
details = parser.to_hash

puts details

#page.save_as 'filename'