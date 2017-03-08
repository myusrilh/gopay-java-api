package cz.gopay.api.test.payment;

import cz.gopay.api.test.utils.TestUtils;
import cz.gopay.api.v3.GPClientException;
import cz.gopay.api.v3.IGPConnector;
import cz.gopay.api.v3.model.access.OAuth;
import cz.gopay.api.v3.model.common.Currency;
import cz.gopay.api.v3.model.common.StatementGeneratingFormat;
import cz.gopay.api.v3.model.eet.EETReceipt;
import cz.gopay.api.v3.model.eet.EETReceiptFilter;
import cz.gopay.api.v3.model.payment.BasePayment;
import cz.gopay.api.v3.model.payment.Lang;
import cz.gopay.api.v3.model.payment.Payment;
import cz.gopay.api.v3.model.payment.BasePaymentBuilder;
import cz.gopay.api.v3.model.payment.PaymentFactory;
import cz.gopay.api.v3.model.payment.PaymentResult;
import cz.gopay.api.v3.model.payment.support.AccountStatement;
import cz.gopay.api.v3.model.payment.support.Payer;
import cz.gopay.api.v3.model.payment.support.PaymentInstrument;
import cz.gopay.api.v3.model.payment.support.PayerBuilder;
import cz.gopay.api.v3.model.payment.support.PaymentInstrumentRoot;
import cz.gopay.api.v3.model.payment.support.Recurrence;
import cz.gopay.api.v3.model.payment.support.RecurrenceCycle;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import junit.framework.Assert;
import org.apache.log4j.Logger;

/**
 *
 * @author František Sichinger
 */
public class AbstractPaymentTests {

    private static final Logger logger = Logger.getLogger(AbstractPaymentTests.class);

    private BasePayment createTestBasePayment() {
        String url = "https://www.eshop123.cz/";
        
        Payer payer = new PayerBuilder().withAllowedPaymentInstruments(Arrays.asList(PaymentInstrument.BANK_ACCOUNT))
                .addAllowedSwift("FIOBCZPP").build();
        BasePaymentBuilder builder = PaymentFactory.createBasePaymentBuilder();
        return builder.withCallback(url+"notify", url+"return")
               .order("123", 10L, Currency.EUR, "description")
               .inLang(Lang.EN)
               .addAdditionalParameter("AKey2", "AValue")
               .addItem("An item", 1L, 1L)
              .toEshop(TestUtils.GOID)
               .payer(payer).build();
    }

    protected long testConnectorCreatePayment(IGPConnector connector) {
        Payment payment = null;
        try {
            BasePayment createpayment = createTestBasePayment();
            payment = connector.getAppToken(TestUtils.CLIENT_ID, TestUtils.CLIENT_SECRET).createPayment(createpayment);
        } catch (GPClientException e) {
            TestUtils.handleException(e, logger);
        }
        Assert.assertNotNull(payment.getId());
        Assert.assertTrue(payment.getId() > 0);
        return payment.getId();
    }

    protected void testPaymentStatus(IGPConnector connector) {
        long id = testConnectorCreatePayment(connector);
        Payment payment = null;
        try {
            payment = connector.getAppToken(TestUtils.CLIENT_ID, TestUtils.CLIENT_SECRET).paymentStatus(id);

        } catch (GPClientException e) {
            TestUtils.handleException(e, logger);
        }

        Assert.assertNotNull(payment.getId());
    }

    protected void testPaymentRefund(IGPConnector connector) {
        try {
            PaymentResult refundPayment = connector.
                    getAppToken(TestUtils.CLIENT_ID, TestUtils.CLIENT_SECRET, OAuth.SCOPE_PAYMENT_ALL).
                    refundPayment(3000027082L, 2L);
            Assert.assertTrue(refundPayment.getId() == 3000027082L);
        } catch (GPClientException ex) {
            TestUtils.handleException(ex, logger);
        }
    }

    protected void testPaymentPreAuthorization(IGPConnector connector) {
        Payment payment = null;
        try {
            BasePayment createpayment = createTestBasePayment();
            createpayment.setPreAuthorization(true);
            payment = connector.getAppToken(TestUtils.CLIENT_ID, TestUtils.CLIENT_SECRET).createPayment(createpayment);
        } catch (GPClientException e) {
            TestUtils.handleException(e, logger);
        }
        Assert.assertNotNull(payment);
        Assert.assertTrue(payment.getId() > 0);
    }

    protected long testPaymentRecurrency(IGPConnector connector) {
        Payment payment = null;
        try {
            
            BasePayment createpayment = createTestBasePayment();
            Recurrence recurrence = new Recurrence();
            recurrence.setRecurrencePeriod(1);
            recurrence.setRecurrenceState(Recurrence.RecurrenceState.STARTED);
            recurrence.setRecurrenceCycle(RecurrenceCycle.WEEK);
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, 2017);
            calendar.set(Calendar.MONTH, 2);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            recurrence.setRecurrenceDateTo(calendar.getTime());
            createpayment.setRecurrence(recurrence);
            payment = connector.getAppToken(TestUtils.CLIENT_ID, TestUtils.CLIENT_SECRET).createPayment(createpayment);
        } catch (GPClientException e) {
            TestUtils.handleException(e, logger);
        }
        Assert.assertNotNull(payment);
        Assert.assertTrue(payment.getId() > 0);
        return payment.getId();
    }

    protected void testPaymentVoidAuthorization(IGPConnector connector) {
        long id = testPaymentRecurrency(connector);
        try {
            PaymentResult voidAuthorization = connector
                    .getAppToken(TestUtils.CLIENT_ID, TestUtils.CLIENT_SECRET, OAuth.SCOPE_PAYMENT_ALL).
                    voidAuthorization(id);
            Assert.assertNotNull(voidAuthorization);
            Assert.assertTrue(voidAuthorization.getId() > 0);
        } catch (GPClientException ex) {
            TestUtils.handleException(ex, logger);
        }
    }
    
    
    protected void testEETREceiptFindByFilter(IGPConnector connector) {
        Calendar calendarFrom = Calendar.getInstance();
        calendarFrom.set(Calendar.DAY_OF_MONTH, 10);
        calendarFrom.set(Calendar.MONTH, 1);
        calendarFrom.set(Calendar.YEAR, 2017);
        
        Calendar calendarTo = Calendar.getInstance();
        calendarTo.set(Calendar.DAY_OF_MONTH, 20);
        calendarTo.set(Calendar.MONTH, 1);
        calendarTo.set(Calendar.YEAR, 2017);
        
        EETReceiptFilter filter = EETReceiptFilter.create(12, calendarFrom.getTime(), calendarTo.getTime());
        
        try {
            List<EETReceipt> receipts = connector.getAppToken(TestUtils.CLIENT_ID, TestUtils.CLIENT_SECRET, OAuth.SCOPE_PAYMENT_CREATE)
                    .findEETREceiptsByFilter(filter);
            Assert.assertNotNull(receipts);
            Assert.assertTrue(!receipts.isEmpty());
        } catch (GPClientException e) {
            TestUtils.handleException(e, logger);
        }
    }
    
    
    protected void testEETReceiptFindByPayment(IGPConnector connector) {
        try {
            List<EETReceipt> receipts = connector.getAppToken(TestUtils.CLIENT_ID, TestUtils.CLIENT_SECRET, OAuth.SCOPE_PAYMENT_CREATE)
                    .getEETReceiptByPaymentId(3000012418L);
            Assert.assertNotNull(receipts);
            Assert.assertTrue(!receipts.isEmpty());
        } catch (GPClientException e) {
            TestUtils.handleException(e, logger);
        }
    }
    
    protected void testPaymentInstrumentRoot(IGPConnector connector){
        try {
            PaymentInstrumentRoot instrumentsList = connector
                    .getAppToken(TestUtils.CLIENT_ID, TestUtils.CLIENT_SECRET, OAuth.SCOPE_PAYMENT_ALL).generatePaymentInstruments(8712700986L, Currency.CZK);
            Assert.assertNotNull(instrumentsList);
        } catch (GPClientException ex) {
            TestUtils.handleException(ex, logger);
        }
    }
    
    
    protected void testGenerateStatement(IGPConnector connector) {
        AccountStatement accountStatement = new AccountStatement();
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2016);
        calendar.set(Calendar.MONTH, 2);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        accountStatement.setDateFrom(calendar.getTime());
        
        calendar.set(Calendar.YEAR, 2016);
        calendar.set(Calendar.MONTH, 4);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        accountStatement.setDateTo(calendar.getTime());
        
        accountStatement.setGoID(TestUtils.GOID);
        accountStatement.setCurrency(Currency.CZK);
        accountStatement.setFormat(StatementGeneratingFormat.CSV_A);
        
        try {
            byte[] statement = connector.getAppToken("1689337452", "CKr7FyEE").generateStatement(accountStatement);
            Assert.assertNotNull(statement);
            
        } catch (GPClientException e) {
            TestUtils.handleException(e, logger);
        }
    }
}
