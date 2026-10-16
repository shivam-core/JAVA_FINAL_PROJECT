package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.dao.LoanDao;
import com.shivamcore.loanapp.exception.ValidationException;
import com.shivamcore.loanapp.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class LoanServiceTest {

    private InMemoryLoanDao loanDao;
    private LoanService loanService;
    private User testUser;

    @BeforeEach
    void setUp() {
        loanDao = new InMemoryLoanDao();
        loanService = new LoanService(loanDao);
        testUser = new User();
        testUser.setId(10L);
        testUser.setUsername("applicant");
    }

    @Test
    void testApplyForLoanSuccess() {
        LoanApplication loan = loanService.applyForLoan(
                testUser,
                new BigDecimal("500000.00"),
                LoanType.HOME,
                60,
                new BigDecimal("1200000.00"),
                "House purchase",
                null
        );

        assertNotNull(loan.getId());
        assertEquals(LoanStatus.PENDING, loan.getStatus());
        assertEquals(new BigDecimal("7.50"), loan.getInterestRate());
        assertEquals(60, loan.getLoanDurationMonths());
    }

    @Test
    void testNegativeAmountRejected() {
        assertThrows(ValidationException.class, () ->
                loanService.applyForLoan(
                        testUser,
                        new BigDecimal("-500.00"),
                        LoanType.PERSONAL,
                        12,
                        new BigDecimal("500000.00"),
                        "Vacation",
                        null
                ));
    }

    @Test
    void testCarAliasMapsToVehicle() {
        LoanType type = LoanType.fromString("car");
        assertEquals(LoanType.VEHICLE, type);
    }

    private static class InMemoryLoanDao implements LoanDao {
        private final Map<Long, LoanApplication> map = new HashMap<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public LoanApplication save(LoanApplication loan) {
            if (loan.getId() == null) {
                loan.setId(idGen.getAndIncrement());
            }
            map.put(loan.getId(), loan);
            return loan;
        }

        @Override
        public Optional<LoanApplication> findById(Long id) {
            return Optional.ofNullable(map.get(id));
        }

        @Override
        public List<LoanApplication> findByUserId(Long userId) {
            List<LoanApplication> list = new ArrayList<>();
            for (LoanApplication l : map.values()) {
                if (l.getUserId().equals(userId)) list.add(l);
            }
            return list;
        }

        @Override
        public List<LoanApplication> findAll() {
            return new ArrayList<>(map.values());
        }

        @Override
        public List<LoanApplication> findByStatus(LoanStatus status) {
            List<LoanApplication> list = new ArrayList<>();
            for (LoanApplication l : map.values()) {
                if (l.getStatus() == status) list.add(l);
            }
            return list;
        }

        @Override
        public void updateStatus(Long id, LoanStatus newStatus, String rejectionReason) {
            LoanApplication l = map.get(id);
            if (l != null) {
                l.setStatus(newStatus);
                l.setRejectionReason(rejectionReason);
            }
        }
    }
}
