package com.atmacanext.app.ai

import org.json.JSONObject

internal object PilotIntentSchema {
    fun value(): JSONObject = JSONObject("""{
      "type":"object","properties":{
        "say":{"type":"string"},
        "actions":{"type":"array","maxItems":4,"items":{"type":"object","properties":{
          "tool":{"type":"string","enum":["GENERATE","TRANSFER","COPY","DELETE_LAST","UNDO_POOL","POOLS","REPORT","STATUS","START","STOP","PAUSE","RESUME","SKIP_TASK","SKIP_ACCOUNT","STOP_GENERATION"]},
          "pool":{"type":"string","enum":["TWEET","REPLY"]},
          "destination":{"type":"string","enum":["TWEET","REPLY"]},
          "count":{"type":"integer","minimum":1,"maximum":10000},
          "concept":{"type":"string"},"account":{"type":"string"},"all":{"type":"boolean"},
          "types":{"type":"array","items":{"type":"string"}}
        },"required":["tool"]}}
      },"required":["say","actions"]
    }""")
}
