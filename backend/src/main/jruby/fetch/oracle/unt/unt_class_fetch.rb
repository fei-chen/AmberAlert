require 'mechanize'

class UNTClassFetch

  def initialize(class_number = '23867',
      institution = 'NT752',
      term = '1138',
      acad_career = 'UGRD',
      fetcher = Mechanize.new {|a| a.user_agent_alias = 'Windows IE 9'}
  )
    @class_number = class_number
    @institution = institution
    @class_search_url = 'https://myls.unt.edu/psc/lspd01/GUEST/HRMS/c/ESTABLISH_COURSES.CLASS_SEARCH.GBL'
    @institution = institution
    @term = term
    @acad_career = acad_career
    @fetcher = fetcher
  end

  # @param [Mechanize::Page] class_search_page
  # @return [Mechanize::Page]
  def submit_class_search(class_search_page)
    class_search_page.form_with(:name => 'win0') do |form|
      form.ICAction = 'CLASS_SRCH_WRK2_SSR_PB_CLASS_SRCH'
      form.add_field!('CLASS_SRCH_WRK2_CLASS_NBR$116$', @class_number)
      form.field_with(:name => 'NTSR_DERIVD_WRK_INSTITUTION').value = @institution
      form.field_with(:name => 'NTSR_DERIVD_WRK_STRM').value = @term
      form.field_with(:name => 'NTSR_DERIVD_WRK_ACAD_CAREER').value = @acad_career
      form.field_with(:name => 'CLASS_SRCH_WRK2_ACAD_CAREER').value = @acad_career
      form.field_with(:name => 'CLASS_SRCH_WRK2_SSR_OPEN_ONLY$chk').value = 'N'
    end.submit
  end

  def select_class(search_results)
    # 'click' on only search result by submitting form with proper action and link number 0
    class_details = search_results.form_with(:name => 'win0') do |form|
      form.ICAction = 'DERIVED_CLSRCH_SSR_CLASSNAME_LONG$0'
    end.submit

    # can output
  end

  def get
    class_search_page = fetch_class_search()
    search_results = submit_class_search(class_search_page)
    select_class(search_results)
  end

  def fetch_class_search
    @fetcher.get(@class_search_url)
  end

end

