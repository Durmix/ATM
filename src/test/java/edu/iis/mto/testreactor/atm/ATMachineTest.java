package edu.iis.mto.testreactor.atm;

import edu.iis.mto.testreactor.atm.bank.AuthorizationException;
import edu.iis.mto.testreactor.atm.bank.Bank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Currency;
import java.util.List;
import java.util.stream.Stream;

import static edu.iis.mto.testreactor.atm.ErrorCode.*;
import static edu.iis.mto.testreactor.atm.Money.DEFAULT_CURRENCY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ATMachineTest {

    private static final Card CARD = Card.create("987456321025846");
    private static final Currency DOLLAR = Currency.getInstance("USD");
    private static final PinCode PIN = PinCode.createPIN(1, 9, 7, 3);

    private MoneyDeposit deposit;
    private ATMachine atm;
    private Money money;
    @Mock
    private Bank bank;

    @BeforeEach
    void setUp() throws Exception {
        List<BanknotesPack> banknotesPacks = List.of(
                BanknotesPack.create(200, Banknote.PL_10),
                BanknotesPack.create(100, Banknote.PL_20),
                BanknotesPack.create(40, Banknote.PL_50),
                BanknotesPack.create(20, Banknote.PL_100),
                BanknotesPack.create(10, Banknote.PL_200),
                BanknotesPack.create(5, Banknote.PL_500)
        );
        deposit = MoneyDeposit.create(DEFAULT_CURRENCY, banknotesPacks);
        atm = new ATMachine(bank, DEFAULT_CURRENCY);
        atm.setDeposit(deposit);
    }

    @Test
    void shouldReturnProperCurrency_PLN() {
        assertEquals(atm.getCurrentDeposit().getCurrency(), DEFAULT_CURRENCY);
    }

    @Test
    void shouldReturnProperCurrency_DOLLAR() {
        ATMachine atm = new ATMachine(bank, DOLLAR);
        assertEquals(atm.getCurrentDeposit().getCurrency(), DOLLAR);
    }

    @Test
    void shouldReturnWithdrawCorrectAmount_250() throws ATMOperationException {
        money = new Money(250, DEFAULT_CURRENCY);
        Withdrawal withdrawal = atm.withdraw(PIN, CARD, money);
        assertEquals(250, withdrawal.getBanknotes().stream().mapToInt(Banknote::getDenomination).sum());
    }

    @Test
    void shouldReturnWithdrawCorrectAmount_1820() throws ATMOperationException {
        money = new Money(1820, DEFAULT_CURRENCY);
        Withdrawal withdrawal = atm.withdraw(PIN, CARD, money);
        assertEquals(1820, withdrawal.getBanknotes().stream().mapToInt(Banknote::getDenomination).sum());
    }

    @Test
    void shouldReturnWithdrawCorrectAmount_AllAvailableMoneyMinusTen() throws ATMOperationException {
        int amount = deposit.getBanknotes().stream().mapToInt(x -> x.getDenomination().getDenomination() * x.getCount()).sum();
        Withdrawal withdrawal = atm.withdraw(PIN, CARD, new Money(amount - 10, DEFAULT_CURRENCY));
        assertEquals(amount - 10, withdrawal.getBanknotes().stream().mapToInt(Banknote::getDenomination).sum());
    }

    @Test
    void shouldReturnWithdrawCorrectAmount_AllAvailableMoney() throws ATMOperationException {
        int amount = deposit.getBanknotes().stream().mapToInt(x -> x.getDenomination().getDenomination() * x.getCount()).sum();
        Withdrawal withdrawal = atm.withdraw(PIN, CARD, new Money(amount, DEFAULT_CURRENCY));
        assertEquals(amount, withdrawal.getBanknotes().stream().mapToInt(Banknote::getDenomination).sum());
    }

    @Test
    void shouldThrowATMOperationException_WrongAmount_IncorrectAmount() {
        money = new Money(137, DEFAULT_CURRENCY);
        ATMOperationException exception = assertThrows(ATMOperationException.class, () -> {
            atm.withdraw(PIN, CARD, money);
        });
        assertEquals(WRONG_AMOUNT, exception.getErrorCode());
    }

    @Test
    void shouldThrowATMOperationException_WrongAmount_AmountGreaterThanDepositInTheMachine() throws ATMOperationException {
        int amount = deposit.getBanknotes().stream().mapToInt(x -> x.getDenomination().getDenomination() * x.getCount()).sum();
        ATMOperationException exception = assertThrows(ATMOperationException.class, () -> {
            atm.withdraw(PIN, CARD, new Money(amount + 10, DEFAULT_CURRENCY));
        });
        assertEquals(WRONG_AMOUNT, exception.getErrorCode());
    }

    @Test
    void shouldThrowATMOperationException_WrongCurrency() {
        money = new Money(170, DOLLAR);
        ATMOperationException exception = assertThrows(ATMOperationException.class, () -> {
            atm.withdraw(PIN, CARD, money);
        });
        assertEquals(WRONG_CURRENCY, exception.getErrorCode());
    }

    @Test
    void shouldThrowATMOperationException_Authorization() throws AuthorizationException {
        money = new Money(150, DEFAULT_CURRENCY);
        Mockito.when(bank.autorize(Mockito.anyString(), Mockito.anyString())).thenThrow(AuthorizationException.class);
        ATMOperationException exception = assertThrows(ATMOperationException.class, () -> {
            atm.withdraw(PIN, CARD, money);
        });
        assertEquals(AUTHORIZATION, exception.getErrorCode());
    }

}
