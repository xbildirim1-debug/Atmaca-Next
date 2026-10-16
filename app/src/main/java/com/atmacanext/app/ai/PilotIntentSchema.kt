package com.atmacanext.app.ai

import org.json.JSONObject

internal object PilotIntentSchema {
    fun value(): JSONObject = JSONObject("""{
      "type":"object","properties":{
        "say":{"type":"string"},
        "actions":{"type":"array","maxItems":4,"items":{"type":"object","properties":{
          "tool":{"type":"string","enum":["GENERATE"]},
          "pool":{"type":"string","enum":["TWEET","REPLY"]},
          "count":{"type":"integer","minimum":1,"maximum":1000},
          "concept":{"type":"string"}
        },"required":["tool","pool","count","concept"]}}
      },"required":["say","actions"]
    }""")
}
