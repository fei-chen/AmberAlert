class SJSUClassParser
  # To change this template use File | Settings | File Templates.
  def initialize(page)
    @page = page
    @selectors = {
        :class_number => '#SSR_CLS_DTL_WRK_CLASS_NBR',
        :title => '#DERIVED_CLSRCH_DESCR200',
        :instructor => 'span[@id="MTG_INSTR$0"]',
        :class_capacity => 'span[@id="SSR_CLS_DTL_WRK_ENRL_CAP"]',
        :enrollment_total => 'span[@id="SSR_CLS_DTL_WRK_ENRL_TOT"]',
        :available_seats => 'span[@id="SSR_CLS_DTL_WRK_AVAILABLE_SEATS"]',
        :description => 'span[@id="DERIVED_CLSRCH_DESCRLONG"]',
        :day_time => 'span[@id="MTG_SCHED$0"]'
    }

    @transforms = {
        :title => Proc.new do |text|
            details = {}
            details[:abrv] = text.split[0]
            details[:course] = text.split[1]
            details[:section] = text.split[3]
            details[:course_title] = text.split[4..-1].join(" ")
            details
          end,
        :day_time => Proc.new do |text|
            details = {}
            details[:days] = text.split[0]
            details[:time] = text.split[1..-1].join(" ")
            details
        end
    }

    @class_details = {}
  end

  def to_hash
    @selectors.each do |k,v|
      text = @page.search(v).text
      if @transforms.has_key? k
        text_transform = @transforms[k]
        details = text_transform.call text
        details.each do |key,value|
          @class_details[key] = value
        end
      else
        @class_details[k] = text
      end
    end
    @class_details
  end

end