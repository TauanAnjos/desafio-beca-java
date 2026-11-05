package br.com.tauan.ms2_transacoes.adapter.out.persistence;

import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, Long> {

    @Query("""
            select sum(t.amount) from TransactionJpaEntity t
            where t.userId = :userId
              and t.status = :status
              and t.type in :types
              and t.occurredAt >= :periodStart
              and t.occurredAt < :periodEnd
            """)
    BigDecimal sumAmount(@Param("userId") Long userId,
                         @Param("status") TransactionStatus status,
                         @Param("types") Collection<TransactionType> types,
                         @Param("periodStart") LocalDateTime periodStart,
                         @Param("periodEnd") LocalDateTime periodEnd);

    @Query("""
            select t from TransactionJpaEntity t
            where t.userId = :userId
              and t.type in :types
              and t.occurredAt >= :periodStart
              and t.occurredAt < :periodEnd
            order by t.occurredAt desc, t.id desc
            """)
    List<TransactionJpaEntity> search(@Param("userId") Long userId,
                                      @Param("types") Collection<TransactionType> types,
                                      @Param("periodStart") LocalDateTime periodStart,
                                      @Param("periodEnd") LocalDateTime periodEnd);
}