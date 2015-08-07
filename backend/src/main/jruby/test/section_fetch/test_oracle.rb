require 'test/unit'
require 'section_fetch'

class OracleFetchTest < Test::Unit::TestCase
  def test_oracle_fetch
    assert_equal "OHNO",
      OracleFetch.get
  end
end
