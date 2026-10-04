package com.example.util

import com.example.data.entity.RuleEntity
import org.json.JSONArray
import org.json.JSONObject

object RuleJsonHelper {

    fun exportRulesToJson(rules: List<RuleEntity>): String {
        val array = JSONArray()
        for (rule in rules) {
            val obj = JSONObject().apply {
                put("id", rule.ruleId.ifEmpty { "user_${rule.id}" })
                put("name", rule.name)
                put("patternType", rule.patternType)
                put("requiredTrend", rule.requiredTrend)
                put("requireNearSupport", rule.requireNearSupport)
                put("requireNearResistance", rule.requireNearResistance)
                put("minConfidence", rule.minConfidence.toDouble())
                put("outcome", rule.outcome)
                put("weight", rule.weight.toDouble())
                put("priorStrength", rule.priorStrength)
                put("priorWeight", rule.priorWeight.toDouble())
                put("winCount", rule.winCount)
                put("lossCount", rule.lossCount)
                put("source", rule.source)
                put("notes", rule.notes)
                put("isEnabled", rule.isEnabled)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    fun parseRulesFromJson(jsonString: String): List<RuleEntity> {
        val rules = mutableListOf<RuleEntity>()
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val weightVal = obj.optDouble("weight", 0.10).toFloat()
            rules.add(
                RuleEntity(
                    ruleId = obj.optString("id", obj.optString("ruleId", "custom_$i")),
                    name = obj.optString("name", "Custom Rule $i"),
                    patternType = obj.optString("patternType", "HAMMER"),
                    requiredTrend = obj.optString("requiredTrend", "ANY"),
                    requireNearSupport = obj.optBoolean("requireNearSupport", false),
                    requireNearResistance = obj.optBoolean("requireNearResistance", false),
                    minConfidence = obj.optDouble("minConfidence", 0.60).toFloat(),
                    outcome = obj.optString("outcome", "UP"),
                    weight = weightVal,
                    priorStrength = obj.optInt("priorStrength", 10),
                    priorWeight = obj.optDouble("priorWeight", weightVal.toDouble()).toFloat(),
                    winCount = obj.optInt("winCount", 0),
                    lossCount = obj.optInt("lossCount", 0),
                    source = obj.optString("source", "user"),
                    notes = obj.optString("notes", ""),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
            )
        }
        return rules
    }
}
