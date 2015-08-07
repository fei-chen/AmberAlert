require 'test/unit'
require 'section_fetch'

class SectionFetchTest < Test::Unit::TestCase
  def test_section_fetch_get
    assert_equal "SectionFetch.get implement me", SectionFetch.get
  end
end
