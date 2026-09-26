package com.yh.toy_pj.controller;

import com.yh.toy_pj.entity.Asset;
import com.yh.toy_pj.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AssetController {

    @Autowired
    private AssetRepository assetRepository;

    // 자산 목록 조회
    @GetMapping("/api/assets")
    public List<Asset> getAssets() {
        return assetRepository.findAll();
    }

    // 자산 등록 (담당자/상태 기본값 세팅)
    @PostMapping("/api/assets")
    public Asset createAsset(@RequestBody Asset asset) {
        if (asset.getStatus() == null || asset.getStatus().isEmpty()) {
            asset.setStatus("사용중");
        }
        if (asset.getOwner() == null || asset.getOwner().isEmpty()) {
            asset.setOwner("미지정");
        }
        return assetRepository.save(asset);
    }

    // 자산 정보 및 상태 수정 (추가된 기능)
    @PutMapping("/api/assets/{id}")
    public Asset updateAsset(@PathVariable Long id, @RequestBody Asset updatedAsset) {
        return assetRepository.findById(id).map(asset -> {
            if (updatedAsset.getName() != null) asset.setName(updatedAsset.getName());
            if (updatedAsset.getType() != null) asset.setType(updatedAsset.getType());
            if (updatedAsset.getOwner() != null) asset.setOwner(updatedAsset.getOwner());
            if (updatedAsset.getStatus() != null) asset.setStatus(updatedAsset.getStatus());
            return assetRepository.save(asset);
        }).orElseThrow(() -> new RuntimeException("자산을 찾을 수 없습니다: " + id));
    }

    // 자산 삭제
    @DeleteMapping("/api/assets/{id}")
    public void deleteAsset(@PathVariable Long id) {
        assetRepository.deleteById(id);
    }
}