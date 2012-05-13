package com.cloudmytask.centralservice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.cloudmytask.GlobalConfig;
import com.cloudmytask.client.Request;
import com.cloudmytask.client.TopologyRequest;
import com.cloudmytask.connectors.CallbackInterface;
import com.cloudmytask.service.MulticastGroup;

public class CentralServiceObject implements CentralPublicServiceInterface, CentralPrivateServiceInterface {

	// un pool de thread-uri pentru a putea prelua cererile primite de la celelalte masini
	// un al 2lea pool de thread-uri pentru a procesa cererile primite
	private ExecutorService evaluateRequestsPool, processIsBannedRequestsPool, processAddToBannedRequestsPool; 
	private ExecutorService processGetAvailableRequestsPool, processUpdateStatusRequestsPool;
	
	// hashmap cu clientii ce trimit scripturi care sunt banned 
	private ConcurrentHashMap<String, Boolean> bannedList;
	private ConcurrentHashMap<String, Integer> loadList;
	
	// **** Multicast stuff
	private MulticastServerHandler multicastHandler = null;
	private TopologyChangeThread topologyChange = null;
	
	public CentralServiceObject() {

		bannedList = new ConcurrentHashMap<String, Boolean>();
		loadList = new ConcurrentHashMap<String, Integer>();
		
		//TODO parametrizare
		this.evaluateRequestsPool = Executors.newFixedThreadPool(4);
		this.processIsBannedRequestsPool = Executors.newFixedThreadPool(4);
		this.processAddToBannedRequestsPool = Executors.newFixedThreadPool(4);
		this.processUpdateStatusRequestsPool = Executors.newFixedThreadPool(4);
		this.processGetAvailableRequestsPool = Executors.newFixedThreadPool(4);
		// ***** Multicast
		try {	
			MulticastGroup group = new MulticastGroup(GlobalConfig.MulticastAddress, GlobalConfig.MulticastPort);
			this.multicastHandler = new MulticastServerHandler(group);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//create topology change watch
		try{
			topologyChange = new TopologyChangeThread(this);
			topologyChange.startRunning();			
		}catch(Exception e){
			System.out.println("[CentralServiceInstance] error in starting topology changed thread lisener " + e.getMessage());
		}
	}
	
	public void sendRequestToCentralUnit(Request request, CallbackInterface ci) {
		// TODO Auto-generated method stub
		
		// creaee thread pt job
		EvaluateRequestJob erj = new EvaluateRequestJob(this, bannedList, loadList, request, ci);
		
		this.evaluateRequestsPool.submit(erj);
	}

	public void processAddToBannedRequest(Request request,  CallbackInterface ci) {
		// TODO Auto-generated method stub
		AddToBannedRequestJob erj = new AddToBannedRequestJob(this, bannedList, request, ci);
		
		this.processAddToBannedRequestsPool.submit(erj);
	}

	public void processGetAvailableRequest(Request request,  CallbackInterface ci) {
		// TODO Auto-generated method stub
		
		GetAvailableRequestJob erj = new GetAvailableRequestJob(this, loadList, request, ci);
		
		this.processGetAvailableRequestsPool.submit(erj);
	}

	public void processIsBannedRequest(Request request, CallbackInterface ci) {
		// TODO Auto-generated method stub
		IsBannedRequestJob erj = new IsBannedRequestJob(this, bannedList, request, ci);
		
		this.processIsBannedRequestsPool.submit(erj);	
	}

	public void processUpdateStatusRequest(Request request, CallbackInterface ci) {
		// TODO Auto-generated method stub
		UpdateStatusRequestJob erj = new UpdateStatusRequestJob(this, loadList, request, ci);
		
		this.processUpdateStatusRequestsPool.submit(erj);	
	}	
	
	
	// Metode de start si stop.
	public void start() {
		// Nothing to do here.
	}
	
	public void stop() {
	/*	this.decryptPool.shutdown();
		this.decodePool.shutdown();
		this.searchCachedResultPool.shutdown();
		this.computeGCDPool.shutdown();
		this.sendResultPool.shutdown();
		this.cacheResultPool.shutdown();
		
		try {
			this.decryptPool.awaitTermination(100000, TimeUnit.MILLISECONDS);
			this.decodePool.awaitTermination(100000, TimeUnit.MILLISECONDS);
			this.searchCachedResultPool.awaitTermination(100000, TimeUnit.MILLISECONDS);
			this.computeGCDPool.awaitTermination(100000, TimeUnit.MILLISECONDS);
			this.cacheResultPool.awaitTermination(100000, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			System.err.println("[GCDServiceObject] Eroare la awaitTermination: " + e);
			e.printStackTrace();
		}*/
	}

	
	public void sendTopology(TopologyRequest update) throws IOException {

		multicastHandler.sendPacket(update);
	}

}
