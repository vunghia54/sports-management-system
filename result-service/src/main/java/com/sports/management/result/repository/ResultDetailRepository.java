package com.sports.management.result.repository;

import com.sports.management.result.model.ResultDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultDetailRepository extends JpaRepository<ResultDetail, Long> {

    List<ResultDetail> findByResultId(Long resultId);

    void deleteByResultId(Long resultId);
}
