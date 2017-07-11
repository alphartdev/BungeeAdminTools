package fr.Alphart.BAT.Utils.thirdparty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.Core.Core;

public class MojangAPIProvider {
  private static Gson gson = new Gson();
  
  private static final String uuidRetrievalUrl = "https://api.mojang.com/users/profiles/minecraft/";
  private static final String nameHistoryUrl = "";
  
  public static String getUUID(final String pName){
    BufferedReader reader = null;
    try{
      final URL url = new URL(uuidRetrievalUrl + pName);
      final URLConnection conn = url.openConnection();
      
      reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      StringBuilder content = new StringBuilder();
      String line = "";
      while((line = reader.readLine()) != null){
        content.append(line);
      }
      
      MojangUUIDProfile p = gson.fromJson(content.toString(), MojangUUIDProfile.class);
      if(p != null && !p.id.isEmpty()){
        return p.id;
      }
    } catch (IOException e) {
      BAT.getInstance().getLogger().log(Level.CONFIG, "Can't retrieve UUID from mojang servers", e);
    } finally{
      if(reader != null){
        try {
          reader.close();
        } catch (IOException ignored) {}
      }
    }
    return null;
  }
  
  /**
   * Fetch a player's name history from <b>Mojang's server : high latency</b>
   * @param pName
   * @throws RuntimeException | if any error is met or if the server is offline mode
   */
  public static List<String> getPlayerNameHistory(final String pName) throws RuntimeException{
      if(!Core.isOnlineMode()){
          throw new RuntimeException("Can't get player name history from an offline server !");
      }
      // Fetch player's name history from Mojang servers
      BufferedReader reader = null;
      try{
          final URL mojangURL = new URL("https://api.mojang.com/user/profiles/" + Core.getUUID(pName) + "/names");
          final URLConnection conn = mojangURL.openConnection();
          reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
          String content = "";
          String line;
          while((line = reader.readLine()) != null){
              content += line;
          }
          final List<String> names = Lists.newArrayList();
          for(final Map<String, Object> entry : 
                  (Set<Map<String, Object>>) gson.fromJson(content, new TypeToken<Set<Map<String, Object>>>() {}.getType())){
              names.add((String)entry.get("name"));
          }
          return names;
      }catch(final IOException e){
          throw new RuntimeException(e);
      }finally{
          if(reader != null){
              try {
                  reader.close();
              } catch (IOException e) {
                  throw new RuntimeException(e);
              }
          }
      }
  }
  
  private class MojangUUIDProfile{
    String id;
    String name;
  }
}