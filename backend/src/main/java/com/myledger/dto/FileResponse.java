package com.myledger.dto;

import com.myledger.entity.StoredFile;

public record FileResponse(Long fileId, String name, String contentType, long size) {
    public static FileResponse from(StoredFile f) {
        return new FileResponse(f.getId(), f.getOriginalName(), f.getContentType(), f.getSizeBytes());
    }
}
