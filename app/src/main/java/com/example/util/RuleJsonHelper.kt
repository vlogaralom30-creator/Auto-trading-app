package com.example.util

import com.example.data.entity.RuleEntity
import org.json.JSONArray
import org.json.JSONObject

object RuleJsonHelper {

    fun exportRulesToJson(rules: List<RuleEntity>): String {
        val array = JSONArray()
        for (rule in rules) {
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("ruleKey", rule.ruleKey)
                put("name", rule.name)
                put("patternType", rule.patternType)
                put("requiredTrend", rule.requiredTrend)
                put("requireNearSupport", rule.requireNearSupport)
                put("requireNearResistance", rule.requireNearResistance)
                put("minConfidence", rule.minConfidence.toDouble())
                put("outcome", rule.outcome)
                put("weight", rule.weight.toDouble())
                put("priorStrength", rule.priorStrength)
                put("priorWinRate", rule.priorWinRate.toDouble())
                put("winCount", rule.winCount)
                put("lossCount", rule.lossCount)
                put("notes", rule.notes)
                put("isEnabled", rule.isEnabled)
                put("source", rule.source)
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
            rules.add(
                RuleEntity(
                    ruleKey = obj.optString("ruleKey", ""),
                    name = obj.optString("name", "Custom Rule $i"),
                    patternType = obj.optString("patternType", "HAMMER"),
                    requiredTrend = obj.optString("requiredTrend", "ANY"),
                    requireNearSupport = obj.optBoolean("requireNearSupport", false),
                    requireNearResistance = obj.optBoolean("requireNearResistance", false),
                    minConfidence = obj.optDouble("minConfidence", 0.60).toFloat(),
                    outcome = obj.optString("outcome", "UP"),
                    weight = obj.optDouble("weight", 0.10).toFloat(),
                    priorStrength = obj.optInt("priorStrength", 10),
                    priorWinRate = obj.optDouble("priorWinRate", 0.50).toFloat(),
                    winCount = obj.optInt("winCount", 0),
                    lossCount = obj.optInt("lossCount", 0),
                    notes = obj.optString("notes", ""),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    source = obj.optString("source", "user")
                )
            )
        }
        return rules
    }
}
