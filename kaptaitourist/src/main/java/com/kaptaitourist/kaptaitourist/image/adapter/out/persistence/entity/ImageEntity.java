package com.kaptaitourist.kaptaitourist.image.adapter.out.persistence.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("khotel_attachment")
public class ImageEntity implements Persistable<String> {
    @Id
    private String id;
    private String hotelId;
    private String fileName;
    private long fileSizeBytes;
    private String fileUrl;
    private boolean isThumbnail;
    private String thumbnailUrl;
    private String mimeType;
    private boolean isPrimary;
    private int displayOrder;
    @Version
    private Long version;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

//    @Override
//    public String getId() {
//        return this.id;
//    }

    @Override
    public boolean isNew() {
        boolean isNull = Objects.isNull(this.id);
        this.id = isNull ? UUID.randomUUID().toString() : this.id;
        return isNull;
    }
//    @Override
//    public String toString() {
//        return CommonFunctions.buildGsonBuilder(this);
//    }

}
