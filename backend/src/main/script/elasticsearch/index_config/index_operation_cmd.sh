#!/usr/bin

curl -XPUT 10.120.227.75:9200/_template/template_course -d '{
    "template" : "course_*"
    "settings" : {
        "number_of_shards" : 5,
        "number_of_replicas": 1
        "analysis":{
          "analyzer":{
            "autocomplete":{
		  "type":"custom",
		  "tokenizer":"standard",
		  "filter":[ "standard", "lowercase", "stop", "kstem", "ngram" ] 
		}
	      },
	      "filter":{
		"ngram":{
		  "type":"ngram",
		  "min_gram":2,
		  "max_gram":15
		}
	      }
	    }
    },
    "mappings" : {
        "info" : {
            "_source" : { "enabled" : true },
            "properties" : {
                "course_id": { "type" : "string" },
                "course_title": { "type" : "string" },
                "professor_name": { "type": "integer" },
                
                "last_status_time": { "type": "long" },
                "quality_score": { "type": "double" },
                "num_products": { "type": "integer" },
                "action": { "type": "integer" },
                "submitted": { "type": "integer" },
                "comments": { "type" : "string", "index" : "not_analyzed" },
                "qa_time": { "type": "long" },
                "metadata": { "type": "binary" }
                
                <department abbr> + <course number>        CS 101
<course title>                        intro to computer science
<department name> + <course number>        Computer Science 101
<professor name>                    Smith
                
            }
        }
    }
}'