package tuwien.taskinn.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.Invoice;

public interface InvoiceInventory extends JpaRepository<Invoice, Long> {
}
