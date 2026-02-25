package com.example.print.db.repository;

import com.example.print.db.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Document} entities storing generated PDFs.
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {
}
