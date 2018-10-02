package autoexpiringkey;

import static org.junit.Assert.*;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;

import com.autoexpiringkey.AutoExpiringStore.AutoExpiringMap;
import com.autoexpiringkey.AutoExpiringStore.AutoExpiringMapImpl;


public class AutoExpiringMapTest {

	private final static int MULTIPLIER = 100;
	
	@Test
	public void basicGetTest() throws InterruptedException {
		AutoExpiringMapImpl testMap;
		testMap = AutoExpiringMapImpl.getInstance(10,5000);
		testMap.put("Sameer", "Father");
		Thread.sleep(MULTIPLIER*40);
		assertEquals("Father",testMap.get("Sameer"));
	}
	
	@Before
	public void resetSingleton() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field instance = AutoExpiringMapImpl.class.getDeclaredField("expiringCache");
		instance.setAccessible(true);
		instance.set(null, null);
	}
	
	@Test
	public void basicExpireTest() throws InterruptedException {
		AutoExpiringMapImpl testMap;
		testMap = AutoExpiringMapImpl.getInstance(10,5000);
		testMap.put("Huzaif", "Son");
		Thread.sleep(MULTIPLIER*50);
		assertNull(testMap.get("Huzaif"));
	}
	
	@Test
	public void basicGetRenewTest() throws InterruptedException {
		AutoExpiringMapImpl testMap;
		testMap = AutoExpiringMapImpl.getInstance(10,5000);
		testMap.put("Huzaif", "Son");
		Thread.sleep(MULTIPLIER*40);
		assertEquals("Son",testMap.get("Huzaif"));
		Thread.sleep(MULTIPLIER*40);		
		assertEquals("Son",testMap.get("Huzaif"));
	}
	
	@Test
	public void basicRenewTest() throws InterruptedException {
		AutoExpiringMapImpl testMap;
		testMap = AutoExpiringMapImpl.getInstance(10,5000);
		testMap.put("Huzaif", "Son");
		Thread.sleep(MULTIPLIER*40);
		testMap.renewKey("Huzaif");
		Thread.sleep(MULTIPLIER*40);		
		assertEquals("Son",testMap.get("Huzaif"));
	}
	
	
	@Test
	public void multiplePutThenGetTest() throws InterruptedException{
		AutoExpiringMapImpl testMap;
		testMap = AutoExpiringMapImpl.getInstance(10,5000);
		testMap.put(1, "abc", 2* MULTIPLIER);
		Thread.sleep(1*MULTIPLIER);
		testMap.put(1, "xyz",2* MULTIPLIER);
		Thread.sleep(1*MULTIPLIER);
		testMap.put(1, "pqr",400 * MULTIPLIER);
		Thread.sleep(MULTIPLIER*2);
		assertEquals("pqr",testMap.get(1));
	}
}