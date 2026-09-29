package tuwien.taskinn.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.InvoiceLine;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
}
