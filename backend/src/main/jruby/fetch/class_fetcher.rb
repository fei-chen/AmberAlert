require 'celluloid'
require '../../../../src/main/jruby/fetch/oracle/sjsu/sjsu_class_fetch'
require '../../../../src/main/jruby/fetch/oracle/sjsu/sjsu_class_parser'

class ClassFetcher
  #include Celluloid

  def initialize(class_number)
    @class_number = class_number
  end

  def fetch
    agent = SJSUClassFetch.new @class_number
    page = agent.get
    #page.save_as "/Users/stormy/hf/findmytext/src/main/jruby/fetch/oracle/sjsu/#{@class_number}.html"
    parser = SJSUClassParser.new page
    details = parser.to_hash
    puts details
  end

end

class_numbers = %w(41310 40483 40484 40485 43680)

class_numbers.each do |class_number|
  begin
    ClassFetcher.new(class_number).fetch
  rescue NoMethodError
    puts "Fetcher failed for #{class_number}"
  end
end
