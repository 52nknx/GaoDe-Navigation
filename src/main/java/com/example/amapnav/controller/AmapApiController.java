package com.example.amapnav.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class AmapApiController {

    @Value("${gaode.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // 经纬度格式正则（xx.xxxx,xx.xxxx）
    private static final Pattern COORD_PATTERN = Pattern.compile("^[0-9.]+,[0-9.]+$");

    /**
     * 地址转坐标通用方法
     */
    private String getLocationFromAddress(String address) {
        String url = String.format(
                "https://restapi.amap.com/v3/geocode/geo?address=%s&key=%s",
                address, apiKey
        );
        String resp = restTemplate.getForObject(url, String.class);
        JSONObject obj = JSON.parseObject(resp);

        if (!"1".equals(obj.getString("status"))) {
            throw new RuntimeException("地址解析失败：" + obj.getString("info"));
        }

        JSONArray geocodes = obj.getJSONArray("geocodes");
        if (geocodes == null || geocodes.isEmpty()) {
            throw new RuntimeException("未找到地址对应的坐标：" + address);
        }

        return geocodes.getJSONObject(0).getString("location");
    }

    /**
     * 统一处理坐标参数（地址自动转坐标）
     */
    private String processLocationParam(String param) {
        return COORD_PATTERN.matcher(param).matches() ? param : getLocationFromAddress(param);
    }

    // 1. 输入提示接口
    @GetMapping("/suggest")
    public ResponseEntity<String> getInputTips(
            @RequestParam String keywords,
            @RequestParam(required = false) String city) {

        String url = "https://restapi.amap.com/v3/assistant/inputtips?keywords=" + keywords +
                "&city=" + (city == null ? "" : city) + "&key=" + apiKey;

        String result = restTemplate.getForObject(url, String.class);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    // 2. 逆地理编码接口
    @GetMapping("/geocode/reverse")
    public ResponseEntity<String> reverseGeocode(
            @RequestParam("location") String location) {

        try {
            String url = "https://restapi.amap.com/v3/geocode/regeo?location=" + location + "&key=" + apiKey;
            String response = restTemplate.getForObject(url, String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("逆地理编码调用失败：" + e.getMessage());
        }
    }

    // 3. 驾车路线规划接口
    @GetMapping("/route/driving")
    public ResponseEntity<String> drivingRoutePlan(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam(required = false) String waypoints) {

        try {
            String originLoc = processLocationParam(origin);
            String destLoc = processLocationParam(destination);

            StringBuilder urlBuilder = new StringBuilder("https://restapi.amap.com/v3/direction/driving");
            urlBuilder.append("?origin=").append(originLoc)
                    .append("&destination=").append(destLoc)
                    .append("&key=").append(apiKey)
                    .append("&extensions=all")  // 返回完整路线信息
                    .append("&strategy=0");

            if (waypoints != null && !waypoints.isEmpty()) {
                urlBuilder.append("&waypoints=").append(waypoints);
            }

            String response = restTemplate.getForObject(urlBuilder.toString(), String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("驾车路线规划失败：" + e.getMessage());
        }
    }

    // 4. 步行路线规划接口（完善地址转坐标功能）
    @GetMapping("/route/walking")
    public ResponseEntity<String> walkingRoutePlan(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination) {

        try {
            // 统一处理地址转坐标（支持地址输入）
            String originLoc = processLocationParam(origin);
            String destLoc = processLocationParam(destination);

            String url = String.format(
                    "https://restapi.amap.com/v3/direction/walking?origin=%s&destination=%s&key=%s&extensions=all",
                    originLoc, destLoc, apiKey
            );
            String response = restTemplate.getForObject(url, String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("步行路线规划失败：" + e.getMessage());
        }
    }

    // 5. 矩形区域实时交通状态查询接口
    @GetMapping("/traffic/status")
    public ResponseEntity<String> trafficStatusQuery(
            @RequestParam("rect") String rect) {

        String url = String.format(
                "https://restapi.amap.com/v3/traffic/status/rectangle?rectangle=%s&key=%s&extensions=all",
                rect, apiKey
        );
        String response = restTemplate.getForObject(url, String.class);
        return ResponseEntity.ok(response);
    }
}