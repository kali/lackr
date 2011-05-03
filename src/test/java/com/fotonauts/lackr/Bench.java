package com.fotonauts.lackr;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.client.CachedExchange;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;

public class Bench {

	private static String[] SOME_IDS = {
		"--lPuDmomd4",
		"--szWy54OUE",
		"-0AQ0Go_BCg",
		"-0CjiIfUgo0",
		"-0JMemfg27E",
		"-0bFt_3xmUA",
		"-0tM9T3rVC4",
		"-11URNWPcvU",
		"-1GIQnxnKmI",
		"-1Q7wb6vCSQ",
		"-1ZW-Z1GUc0",
		"-2-FdQxoxRA",
		"-24I8xWzmRM",
		"-2D5BkFr6hg",
		"-2YUsSw9MVM",
		"-3101rk5ObM",
		"-3S-YbojBeA",
		"-3UJtBVSSYY",
		"-4562OlTmZY",
		"-48BLbFQu5E",
		"-4DP58FLppQ",
		"-4PUumPC5-k",
		"-4X4v-xOtH4",
		"-4oGOHhQ8C8",
		"-5E-gBlT_UI",
		"-5RnvBJYdoQ",
		"-5Tt7Na2OA8",
		"-5dY5rGP3AI",
		"-5vPSq19jL4",
		"-6UjyFiLC6w",
		"-6c52Q5_A5I",
		"-6cwuRWZwN8",
		"-75bINRbXpo",
		"-7hXRfPP4pU",
		"-7rwpvX4T60",
		"-7tDWxbrRUA",
		"-8CZMZJjkBs",
		"-8CjsEr23uQ",
		"-8epTkYeP7c",
		"-8o2mvnjUsI",
		"-8qGE6QP-ak",
		"-8r4Dkqd-OE",
		"-8vObHeONGE",
		"-94C9S3ZlsA",
		"-9KvjuuacbI",
		"-9Ot_xcTXxk",
		"-9gqY4Nt2s0",
		"-9qyJFzwLYc",
		"-9rfT7EM_64",
		"-A2Ea7FlldU",
		"-AM42bEZoz0",
		"-AjwCBI7Zv4",
		"-AlfF7I7mtk",
		"-B55Bm1T9fQ",
		"-B5nmayTPJs",
		"-BFY1FomGAY",
		"-BRV-SbMGxI",
		"-BX7umMAzLY"
	};
	
	public static void main(String[] args) throws Exception {
		int number = 10000000;
		int concurrency = 1;
//		warm();
//		while (concurrency < 512) {
			bench(number, 256);
			concurrency <<= 1;
//		}
	}
	
	public static void warm() throws Exception {
		HttpClient client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		client.setMaxConnectionsPerAddress(4);
		client.start();
		
		final CountDownLatch latch = new CountDownLatch(SOME_IDS.length);
		
		for(String id: SOME_IDS) {
			ContentExchange exchange = new ContentExchange(false) {
				@Override
				protected void onException(Throwable x) {
					System.err.println("failed: " + x);
					x.printStackTrace(System.err);
				}

				@Override
				protected void onResponseComplete() throws IOException {
					super.onResponseComplete();
					latch.countDown();
				}
			};			
			exchange.setURL("http://www.virtual.ftnz.net:8000/medias/" + id + "-crop_24x24/esi/present/ModelPresenter::MediaPresenter/urlsrc");
			exchange.addRequestHeader("X-Ftn-inline-images", "yes");
			exchange.addRequestHeader("X-nginx-ssi", "force");
			client.send(exchange);
		}
		latch.await();
	}

	public static void bench(int number, int concurrency) throws Exception {
		HttpClient client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		client.setMaxConnectionsPerAddress(concurrency);
		client.start();

		Date start = new Date();
		final CountDownLatch todo = new CountDownLatch(number);
		final AtomicInteger active = new AtomicInteger();
		for (int i = 0; i < number; i++) {
			
			while(active.intValue() > 1000)
				Thread.yield();
			
			CachedExchange exchange = new ContentExchange(false) {
				@Override
				protected void onConnectionFailed(Throwable x) {
					System.err.println("failed: " + x);
					x.printStackTrace(System.err);
				}
				@Override
				protected void onException(Throwable x) {
					System.err.println("failed: " + x);
				}

				@Override
				protected void onResponseComplete() throws IOException {
					todo.countDown();
					active.decrementAndGet();
					super.onResponseComplete();
				}
			};
			exchange.setURL("http://www.virtual.ftnz.net:8000/medias/" + SOME_IDS[(int) (Math.random() * SOME_IDS.length)] + "-crop_24x24/esi/present/ModelPresenter::MediaPresenter/urlsrc");
			exchange.addRequestHeader("X-Ftn-inline-images", "yes");
			exchange.addRequestHeader("X-nginx-ssi", "force");
			client.send(exchange);
			active.incrementAndGet();
		}

		todo.await();
		Date stop = new Date();
		long totalms = stop.getTime() - start.getTime();
		System.out.println("httpclient concurrency: " + concurrency + " total time: " + totalms + " ms   "
		        + "mean time: " + ((float) totalms / number * concurrency) + " ms");
	}
}
