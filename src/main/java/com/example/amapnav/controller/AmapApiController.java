package com.example.amapnav.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.regex.Pattern;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class AmapApiController {

    @Value("${gaode.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public AmapApiController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // 经纬度格式正则（支持负数和空格，如 116.39748,39.90882）
    private static final Pattern COORD_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?$");

    private void ensureApiKeyConfigured() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("未配置高德 Web 服务 Key，请设置环境变量 GAODE_KEY 或 application.properties 中的 gaode.key");
        }
    }

    private URI amapUri(UriComponentsBuilder builder) {
        ensureApiKeyConfigured();
        return builder
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();
    }

    private ResponseEntity<String> okJson(String body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private ResponseEntity<String> errorJson(String message) {
        JSONObject body = new JSONObject();
        body.put("status", "0");
        body.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body.toJSONString());
    }

    /**
     * 地址转坐标通用方法
     */
    private String getLocationFromAddress(String address) {
        URI uri = amapUri(
                UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/geocode/geo")
                        .queryParam("address", address)
        );
        String resp = restTemplate.getForObject(uri, String.class);
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
        String value = param == null ? "" : param.trim();
        return COORD_PATTERN.matcher(value).matches() ? value.replaceAll("\\s+", "") : getLocationFromAddress(value);
    }

    // 1. 输入提示接口
    @GetMapping("/suggest")
    public ResponseEntity<String> getInputTips(
            @RequestParam String keywords,
            @RequestParam(required = false) String city) {

        try {
            URI uri = amapUri(
                    UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/assistant/inputtips")
                            .queryParam("keywords", keywords)
                            .queryParam("city", city == null ? "" : city)
            );
            String result = restTemplate.getForObject(uri, String.class);
            return okJson(result);
        } catch (Exception e) {
            return errorJson("输入提示调用失败：" + e.getMessage());
        }
    }

    // 2. 逆地理编码接口
    @GetMapping("/geocode/reverse")
    public ResponseEntity<String> reverseGeocode(
            @RequestParam("location") String location) {

        try {
            URI uri = amapUri(
                    UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/geocode/regeo")
                            .queryParam("location", location)
            );
            String response = restTemplate.getForObject(uri, String.class);
            return okJson(response);
        } catch (Exception e) {
            return errorJson("逆地理编码调用失败：" + e.getMessage());
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

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://restapi.amap.com/v3/direction/driving")
                    .queryParam("origin", originLoc)
                    .queryParam("destination", destLoc)
                    .queryParam("extensions", "all")
                    .queryParam("strategy", "0");

            if (waypoints != null && !waypoints.isEmpty()) {
                builder.queryParam("waypoints", waypoints.trim());
            }

            String response = restTemplate.getForObject(amapUri(builder), String.class);
            return okJson(response);
        } catch (Exception e) {
            return errorJson("驾车路线规划失败：" + e.getMessage());
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

            URI uri = amapUri(
                    UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/direction/walking")
                            .queryParam("origin", originLoc)
                            .queryParam("destination", destLoc)
                            .queryParam("extensions", "all")
            );
            String response = restTemplate.getForObject(uri, String.class);
            return okJson(response);
        } catch (Exception e) {
            return errorJson("步行路线规划失败：" + e.getMessage());
        }
    }

    // 5. 矩形区域实时交通状态查询接口
    @GetMapping("/traffic/status")
    public ResponseEntity<String> trafficStatusQuery(
            @RequestParam("rect") String rect) {

        try {
            URI uri = amapUri(
                    UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/traffic/status/rectangle")
                            .queryParam("rectangle", rect)
                            .queryParam("extensions", "all")
            );
            String response = restTemplate.getForObject(uri, String.class);
            return okJson(response);
        } catch (Exception e) {
            return errorJson("交通状态查询失败：" + e.getMessage());
        }
    }
}
