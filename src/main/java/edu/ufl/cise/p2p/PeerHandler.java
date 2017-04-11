package edu.ufl.cise.p2p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import edu.ufl.cise.p2p.message.Choke;
import edu.ufl.cise.p2p.message.Unchoke;

public class PeerHandler{

    PreferredNeighbours preferredNeighboursTask;
    OptimisticallyUnchokedNeighbour optimisticallyUnchokedTask;
    CommonPeerProperties commonProp;

    public PeerHandler(ArrayList<RemotePeer> peers, CommonPeerProperties prop, Boolean hasFile, int peerId){

        optimisticallyUnchokedTask = new OptimisticallyUnchokedNeighbour(prop,peerId);
        preferredNeighboursTask = new PreferredNeighbours(peers, prop, hasFile, optimisticallyUnchokedTask,peerId);
        commonProp = prop;

    }

    /*Method to schedule periodic tasks for updating the preferred neighbours
     and optimistically unchoked neighbour*/

    public void sendChokeAndUnchokeMessages(){
        final ScheduledExecutorService preferredNeighbourSched = Executors.newScheduledThreadPool(1);
        final ScheduledExecutorService optUnchokedSched = Executors.newScheduledThreadPool(1);

        preferredNeighbourSched.scheduleAtFixedRate(preferredNeighboursTask,0,
                commonProp.getUnchokingInterval(), TimeUnit.SECONDS);

        optUnchokedSched.scheduleAtFixedRate(optimisticallyUnchokedTask,0,
                commonProp.getOptimisticUnchokingInterval(), TimeUnit.SECONDS);
    }

    class PreferredNeighbours implements Runnable{

        ArrayList<RemotePeer> remotePeers;
        CommonPeerProperties commonProp;
        AtomicBoolean hasFile = new AtomicBoolean(false);
        ArrayList<RemotePeer> interestedPeers;
        ArrayList<RemotePeer> preferredNeighbours;
        OptimisticallyUnchokedNeighbour optUnchokedScheduler;
        ArrayList<RemotePeer> optUnchokedNeighbours;
        int localPeerId;

        public PreferredNeighbours(ArrayList<RemotePeer> peers, CommonPeerProperties prop,
                                   Boolean hasFile,
                                   OptimisticallyUnchokedNeighbour optUnchokedScheduler,int peerId){
            remotePeers = peers;
            commonProp = prop;
            this.hasFile.set(hasFile);
            this.optUnchokedScheduler = optUnchokedScheduler;
            preferredNeighbours = new ArrayList<RemotePeer>();
            optUnchokedNeighbours = new ArrayList<RemotePeer>();
            localPeerId = peerId;
        }

        public void run(){
            interestedPeers = getInterestedRemotePeers();

            if(interestedPeers.size() != 0){
                if(hasFile.get()){
                    Collections.shuffle(interestedPeers);
                }
                else{
                    Collections.sort(interestedPeers, new Comparator<RemotePeer>() {
                        public int compare(RemotePeer r1, RemotePeer r2) {
                            // Sort in decreasing order
                            return (r2.getBytesDownloaded().get() - r1.getBytesDownloaded().get());
                        }
                    });
                }

                for(RemotePeer i : remotePeers)
                    i.getBytesDownloaded().set(0);

                clearPreviousPreferredNeighbours(); //clearing previous preferredNeighbours from the list
                preferredNeighbours.addAll(interestedPeers.subList(0,
                        Math.min(commonProp.getNumberOfPreferredNeighbors(),interestedPeers.size())));  //updating preferredNeighbours

                LinkedList<RemotePeer> chokedPeers = new LinkedList<RemotePeer>(remotePeers);
                chokedPeers.removeAll(preferredNeighbours);

                optUnchokedNeighbours.clear();
                optUnchokedNeighbours.addAll(interestedPeers.subList(preferredNeighbours.size(),interestedPeers.size()));

                optUnchokedScheduler.updateOptUnchokeablePeers(optUnchokedNeighbours);

                for(RemotePeer chokedPeer: chokedPeers){
                    if(!chokedPeer.getIsChoked().get() && chokedPeer.getConnection() != null) {
                        chokedPeer.getConnection().sendMessage(new Choke());    //sending choke message to choked peers
                        chokedPeer.getIsChoked().set(true);
                        chokedPeer.getIsUnchoked().set(false);
                    }
                }

                for(RemotePeer preferredNeighbour: preferredNeighbours){
                    if(!preferredNeighbour.getIsUnchoked().get()){
                        preferredNeighbour.getConnection().sendMessage(new Unchoke());  //sending unchoke to preferred neighbours
                        preferredNeighbour.getIsUnchoked().set(true);
                        preferredNeighbour.getIsChoked().set(false);
                    }
                }
            }

        }

        private ArrayList<RemotePeer> getInterestedRemotePeers(){
            ArrayList<RemotePeer> temp = new ArrayList<RemotePeer>();
            for(int i = 0; i < remotePeers.size(); i++){
                if(remotePeers.get(i).getIsInterested().get()){
                    temp.add(remotePeers.get(i));
                }
            }
            return temp;
        }

        private void clearPreviousPreferredNeighbours(){
            for(RemotePeer i : preferredNeighbours)
                i.getIsPreferredNeighbor().set(false);

            preferredNeighbours.clear();
        }
    }

    class OptimisticallyUnchokedNeighbour implements Runnable{

        ArrayList<RemotePeer> optUnchokeablePeers;
        CommonPeerProperties commonProp;
        RemotePeer optimisticallyUnchokedNeighbour;
        int localPeerId;

        public final ReentrantLock unchokeablePeersLock;

        public OptimisticallyUnchokedNeighbour(CommonPeerProperties prop,int peerId){
            commonProp = prop;
            unchokeablePeersLock = new ReentrantLock();
            optUnchokeablePeers = new ArrayList<RemotePeer>();
            localPeerId = peerId;
        }
        public void run(){
            unchokeablePeersLock.lock();


            if(optUnchokeablePeers.size() != 0){
                Random rand = new Random();
                if(optimisticallyUnchokedNeighbour != null)
                    optimisticallyUnchokedNeighbour.getIsOptimisticallyUnchoked().set(false);


                //Randomly selecting optimisticUnchokedNeighbour
                optimisticallyUnchokedNeighbour = optUnchokeablePeers.get(rand.nextInt(optUnchokeablePeers.size()));


                if(!optimisticallyUnchokedNeighbour.getIsUnchoked().get()){


                    optimisticallyUnchokedNeighbour.getConnection().sendMessage(new Unchoke());

//                  System.out.println("SENDING UNCHOKE TO optimisticallyUnchokedNeighbour");
                    optimisticallyUnchokedNeighbour.getIsOptimisticallyUnchoked().set(true);
                    optimisticallyUnchokedNeighbour.getIsUnchoked().set(true);
                    optimisticallyUnchokedNeighbour.getIsChoked().set(false);
                }

            }

            unchokeablePeersLock.unlock();

        }

        
        public void updateOptUnchokeablePeers(ArrayList<RemotePeer> unchokeablePeers){
            unchokeablePeersLock.lock();
            optUnchokeablePeers = unchokeablePeers;
            unchokeablePeersLock.unlock();
        }
    }

}
