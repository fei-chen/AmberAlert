require 'mechanize'

class SJSUClassFetch

  def initialize(class_number = '49083',
      institution = 'SJ000',
      term = '2134',
      acad_career = 'UGRD',
      fetcher = Mechanize.new {|a| a.user_agent_alias = 'Windows IE 9'}
  )
    @class_number = class_number
    @class_search_url = 'https://cmshr.cms.sjsu.edu/psc/HSJPRDF/EMPLOYEE/HSJPRD/c/COMMUNITY_ACCESS.CLASS_SEARCH.GBL?pslnkid=SJ_CLASS_SRCH_LNK&amp;PortalActualURL=https%3a%2f%2fcmshr.cms.sjsu.edu%2fpsc%2fHSJPRDF%2fEMPLOYEE%2fHSJPRD%2fc%2fCOMMUNITY_ACCESS.CLASS_SEARCH.GBL%3fpslnkid%3dSJ_CLASS_SRCH_LNK&amp;PortalContentURL=https%3a%2f%2fcmshr.cms.sjsu.edu%2fpsc%2fHSJPRDF%2fEMPLOYEE%2fHRMS%2fc%2fCOMMUNITY_ACCESS.CLASS_SEARCH.GBL%3fpslnkid%3dSJ_CLASS_SRCH_LNK&amp;PortalContentProvider=HRMS&amp;PortalCRefLabel=Class%20Search&amp;PortalRegistryName=EMPLOYEE&amp;PortalServletURI=https%3a%2f%2fcmshr.cms.sjsu.edu%2fpsp%2fHSJPRDF%2f&amp;PortalURI=https%3a%2f%2fcmshr.cms.sjsu.edu%2fpsc%2fHSJPRDF%2f&amp;PortalHostNode=HRMS&amp;NoCrumbs=yes&amp;PortalKeyStruct=yes'
    @institution = institution
    @term = term
    @acad_career = acad_career
    @fetcher = fetcher
  end

  def submit_class_search(class_search_page)
    class_search_page.form_with(:name => 'win0') do |form|
      form.ICAction = 'CLASS_SRCH_WRK2_SSR_PB_CLASS_SRCH' # class search action
      form.field_with(:name => 'CLASS_SRCH_WRK2_INSTITUTION$51$').value = @institution # sjsu, sjsu-offsite, etc
      form.field_with(:name => 'CLASS_SRCH_WRK2_STRM$54$').value = @term # fall, spring, summer, etc.
      form.field_with(:name => 'CLASS_SRCH_WRK2_ACAD_CAREER').value = @acad_career # undergrad, grad, etc.
      form.add_field!('CLASS_SRCH_WRK2_CLASS_NBR$114$', @class_number) # this uniquely ids a class
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
    # search_results.search("Span[title='SJSU Bookstore Link'] a").first.value gives bookstore link
    select_class(search_results)
  end

  def fetch_class_search
    puts "Fetching class search page for class number: #{@class_number}"
    @fetcher.get(@class_search_url)
  end

end