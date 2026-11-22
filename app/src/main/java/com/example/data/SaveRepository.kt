package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import com.example.model.AchievementDef
import com.example.model.AppLanguage
import com.example.model.DogSkin
import com.example.model.GameSettings
import com.example.model.PenModel
import com.example.model.SaveProgress
import org.json.JSONArray
import org.json.JSONObject

object GameCatalog {
  val skins =
    listOf(
      DogSkin(
        id = "shiba",
        name = "Sunny Shiba",
        cost = 0,
        furColor = Color(0xFFF39C2E),
        muzzleColor = Color(0xFFFFF4DE),
        earColor = Color(0xFFD97B18),
        accessory = "none",
      ),
      DogSkin(
        id = "corgi",
        name = "Caramel Corgi",
        cost = 60,
        furColor = Color(0xFFE58A48),
        muzzleColor = Color(0xFFFFFFFF),
        earColor = Color(0xFFB85E24),
        accessory = "collar",
      ),
      DogSkin(
        id = "husky",
        name = "Frost Husky",
        cost = 120,
        furColor = Color(0xFF6B7C93),
        muzzleColor = Color(0xFFF4F7FA),
        earColor = Color(0xFF455469),
        accessory = "scarf",
      ),
      DogSkin(
        id = "golden",
        name = "Honey Pup",
        cost = 200,
        furColor = Color(0xFFF7C244),
        muzzleColor = Color(0xFFFFF8E1),
        earColor = Color(0xFFD99A1E),
        accessory = "bow",
      ),
      DogSkin(
        id = "ninja",
        name = "Ninja Doge",
        cost = 320,
        furColor = Color(0xFF3E4554),
        muzzleColor = Color(0xFFEADBC8),
        earColor = Color(0xFF232730),
        accessory = "headband",
      ),
      DogSkin(
        id = "royal",
        name = "King Bark",
        cost = 480,
        furColor = Color(0xFFFFA726),
        muzzleColor = Color(0xFFFFF3E0),
        earColor = Color(0xFFE65100),
        accessory = "crown",
      ),
    )

  val achievements =
    listOf(
      AchievementDef("first_save", "First Rescue", "Complete Level 1", 25, 1),
      AchievementDef("ten_wins", "Pup Protector", "Complete 10 levels", 60, 10),
      AchievementDef("twenty_five_wins", "Hive Buster", "Complete 25 levels", 120, 25),
      AchievementDef("fifty_wins", "Master Architect", "Complete 50 levels", 250, 50),
      AchievementDef("stars_30", "Shining Bright", "Collect 30 total stars", 75, 30),
      AchievementDef("stars_100", "Star Guardian", "Collect 100 total stars", 200, 100),
      AchievementDef("ink_saver", "Minimalist", "Win a level with 3 stars", 40, 1),
      AchievementDef("fashion_pup", "Fashion Pup", "Unlock an extra dog skin", 50, 2),
    )

  val penModels =
    listOf(
      PenModel(
        id = "red",
        name = "Red",
        barrelColor = Color(0xFFE53935),
        accentColor = Color(0xFFFFCDD2),
        inkColor = Color(0xFFD32F2F),
      ),
      PenModel(
        id = "green",
        name = "Green",
        barrelColor = Color(0xFF43A047),
        accentColor = Color(0xFFC8E6C9),
        inkColor = Color(0xFF2E7D32),
      ),
      PenModel(
        id = "yellow",
        name = "Yellow",
        barrelColor = Color(0xFFFBC02D),
        accentColor = Color(0xFFFFF9C4),
        inkColor = Color(0xFFE69F00),
      ),
      PenModel(
        id = "black",
        name = "Black",
        barrelColor = Color(0xFF2D313D),
        accentColor = Color(0xFFB0BEC5),
        inkColor = Color(0xFF1F222A),
      ),
    )

  fun findSkin(id: String): DogSkin = skins.firstOrNull { it.id == id } ?: skins.first()

  fun findPenModel(id: String): PenModel =
    penModels.firstOrNull { it.id == id } ?: penModels.last()
}

class SaveRepository(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("save_the_dog_prefs", Context.MODE_PRIVATE)

  fun load(): SaveProgress {
    val raw = prefs.getString(KEY_SAVE, null) ?: return SaveProgress()
    return try {
      val json = JSONObject(raw)
      val starsObj = json.optJSONObject("stars") ?: JSONObject()
      val starsMap = mutableMapOf<Int, Int>()
      starsObj.keys().forEach { key ->
        key.toIntOrNull()?.let { lvl -> starsMap[lvl] = starsObj.optInt(key, 0) }
      }
      val skinsArr = json.optJSONArray("skins") ?: JSONArray().apply { put("shiba") }
      val skinsSet = mutableSetOf("shiba")
      for (i in 0 until skinsArr.length()) {
        skinsSet.add(skinsArr.optString(i, "shiba"))
      }
      val achArr = json.optJSONArray("achievements") ?: JSONArray()
      val achSet = mutableSetOf<String>()
      for (i in 0 until achArr.length()) {
        achSet.add(achArr.optString(i))
      }
      val settingsObj = json.optJSONObject("settings") ?: JSONObject()
      val langCode = settingsObj.optString("language", "en")
      val lang = AppLanguage.entries.firstOrNull { it.code == langCode } ?: AppLanguage.EN
      val penId = settingsObj.optString("selectedPenId", "black")

      SaveProgress(
        unlockedLevel = json.optInt("unlockedLevel", 1).coerceIn(1, 100),
        starsPerLevel = starsMap,
        coins = json.optInt("coins", 50).coerceAtLeast(0),
        unlockedSkins = skinsSet,
        selectedSkinId = json.optString("selectedSkinId", "shiba"),
        unlockedAchievements = achSet,
        totalWins = json.optInt("totalWins", 0),
        settings =
          GameSettings(
            soundEnabled = settingsObj.optBoolean("sound", true),
            musicEnabled = settingsObj.optBoolean("music", true),
            vibrationEnabled = settingsObj.optBoolean("vibration", true),
            highGraphics = settingsObj.optBoolean("highGraphics", true),
            language = lang,
            selectedPenId = penId,
          ),
      )
    } catch (_: Exception) {
      SaveProgress()
    }
  }

  fun save(progress: SaveProgress) {
    val json = JSONObject()
    json.put("unlockedLevel", progress.unlockedLevel)
    val starsObj = JSONObject()
    progress.starsPerLevel.forEach { (lvl, s) -> starsObj.put(lvl.toString(), s) }
    json.put("stars", starsObj)
    json.put("coins", progress.coins)
    json.put("skins", JSONArray(progress.unlockedSkins.toList()))
    json.put("selectedSkinId", progress.selectedSkinId)
    json.put("achievements", JSONArray(progress.unlockedAchievements.toList()))
    json.put("totalWins", progress.totalWins)

    val sObj = JSONObject()
    sObj.put("sound", progress.settings.soundEnabled)
    sObj.put("music", progress.settings.musicEnabled)
    sObj.put("vibration", progress.settings.vibrationEnabled)
    sObj.put("highGraphics", progress.settings.highGraphics)
    sObj.put("language", progress.settings.language.code)
    sObj.put("selectedPenId", progress.settings.selectedPenId)
    json.put("settings", sObj)

    prefs.edit { putString(KEY_SAVE, json.toString()) }
  }

  fun reset(): SaveProgress {
    val fresh = SaveProgress()
    save(fresh)
    return fresh
  }

  companion object {
    private const val KEY_SAVE = "progress_v1"
  }
}
