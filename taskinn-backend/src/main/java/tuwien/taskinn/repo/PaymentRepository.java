package tuwien.taskinn.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tuwien.taskinn.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
