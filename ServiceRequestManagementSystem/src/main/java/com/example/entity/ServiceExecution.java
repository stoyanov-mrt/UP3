package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Planned and actual execution of the service for one request. Created when the request is scheduled
 * and changed only through {@link ServiceRequest}.
 */
@Entity
@Table(name = "service_executions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA
public class ServiceExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime expectedStart;

    @Column(nullable = false)
    private LocalDateTime expectedEnd;

    private LocalDateTime actualStart;

    private LocalDateTime actualEnd;

    @Column(length = 2000)
    private String result;

    ServiceExecution(LocalDateTime expectedStart, LocalDateTime expectedEnd) {
        this.expectedStart = expectedStart;
        this.expectedEnd = expectedEnd;
    }

    void start(LocalDateTime now) {
        actualStart = now;
    }

    void complete(String result, LocalDateTime now) {
        this.result = result;
        actualEnd = now;
    }
}
