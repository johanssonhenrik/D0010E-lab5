package lab5.carwash;
import lab5.simulator.Event;
import lab5.simulator.SimState;
import lab5.simulator.SortedSequence;

public class CarWashEvent extends Event {
	
	private Car car;
	private CarWashState CWS;
	private String eventType = "";
	private double washTime;

	public CarWashEvent(String eventType, SimState SS, SortedSequence SSeq){	//ARRIVE
		this.eventType = eventType;
		CWS = (CarWashState) SS;
		this.car = CarFactory.newCar();
	}
	public CarWashEvent(String eventType, SimState SS, Car car, double time, double washTime){	//LEAVE
		this.eventType = eventType;
		CWS = (CarWashState) SS;
		this.car = car;
	}
	public CarWashEvent(String eventType, SimState SS){						//START
		this.eventType = eventType;
		CWS = (CarWashState) SS;
		time = 0.00;
	}
													//Dummy variable
	public CarWashEvent(String eventType, SimState SS, Boolean isStop){		//STOP
		this.eventType = eventType;
		time = 15.00;
	}
	
	public void Execute(SortedSequence SSeq, SimState SS){
		
		if(this.eventType == "START"){
			CarWashState.currentEvent = "START";
			SS.setRUNNING(true);
			SSeq.sortEvents(new CarWashEvent("ARRIVE",SS, SSeq));	//Skapar ett nytt ARRIVE-Event medans ett ARRIVE-Event k�rs.
			CWS.observable(this); // kanske					//Skickar in nuvarande Event till SimState som i sin tur uppdaterar.
		}
		if(this.eventType == "STOP"){
			CarWashState.currentEvent = "STOP";
			System.out.println(SS.isRunning()+"INNAN");
			SS.setRUNNING(false);
			System.out.println(SS.isRunning()+"EFTER");
			CarWashState CWS = (CarWashState) SS;
			CWS.updateTotalQueueTime(this);
			CWS.observable(this);
		}
		if(this.eventType == "ARRIVE"){
			CarWashState.currentEvent = "ARRIVE";
			SSeq.sortEvents(new CarWashEvent("ARRIVE",SS, SSeq));
			CWS.updateTotalQueueTime(this);
			
			if(CarWashState.fastAvailable()){
				CarWashState.availableFastMachines--;
				car.previousMachine = "fast";
				washTime = CWS.getFastWashTime();								//Ber�kna washTime f�r detta Arrive-Objekt (FAST Machine)
				SSeq.sortEvents(new CarWashEvent("LEAVE",CWS,car,time,washTime)); //Skapar ett nytt leave objekt som f�r all ARRIVE info.
				CWS.updateTotalIdleTime(this);
				CWS.observable(this);
				
			}else if(CarWashState.slowAvailable()){
				CarWashState.availableSlowMachines--;
				car.previousMachine = "slow";
				washTime = CWS.getSlowWashTime();	//Ber�kna washTime f�r detta Arrive-Objekt (SLOW Machine)
				//System.out.println();
				SSeq.sortEvents(new CarWashEvent("LEAVE",CWS,car,time,washTime));
				CWS.updateTotalIdleTime(this);
				CWS.observable(this);
				
			}else if((CarWashState.fastAvailable() == false && CarWashState.slowAvailable() == false) && FIFO.carQueue.size()<FIFO.maxSize()){	//Om FIFO:n inte �r full
				car.previousMachine = "FIFO";
				CWS.observable(this);
				FIFO.add(new CarWashEvent("LEAVE",CWS,car,time,washTime));
			}else{
				CWS.observable(this);
				CarWashState.rejectedCars++;
			}
		}
		if(this.eventType == "LEAVE"){
			
			//System.out.println("LEAVE EXECUTE");
			//Om leave inte �r i fast eller slow. S� m�ste den v�nta p� att dess arrive ska ske. Som i sin tur �ndrar p� previousmachine till antingen fast eller slow.
			
			CarWashState.currentEvent = "LEAVE";
			CWS.updateTotalQueueTime(this);
			if(car.previousMachine == "Fast"){
				CarWashState.availableFastMachines++;
				if(FIFO.isEmpty() == false){	//Vi (Endast d�r vi kan plocka ifr�n)prioriterar bilar som �r i FIFOn
					/*CarWashEvent firstInLine = FIFO.getFirst();
					FIFO.removeFirst();	//Index 0?
					firstInLine.time = this.time + CWS.getFastWashTime();	//Detta Leave event var i en Fast maskin tidigare. P� s� s�tt vet vi att en fast �r tom. (R�knar ut en fast wash time)
					firstInLine.car.previousMachine = "Fast";
					CarWashState.availableFastMachines--;
					SSeq.sortEvents(firstInLine);
					CWS.observable(this);*/
				}
			}
			if(car.previousMachine == "Slow"){
				CarWashState.availableSlowMachines++;
				if(FIFO.isEmpty() == false){	//Vi prioriterar bilar som �r i FIFOn
					/*CarWashEvent firstInLine = FIFO.getFirst();
					FIFO.removeFirst();
					firstInLine.time = this.time + CWS.getSlowWashTime();	//Detta Leave event var i en Fast maskin tidigare. P� s� s�tt vet vi att en fast �r tom. (R�knar ut en fast wash time)
					firstInLine.car.previousMachine = "Slow";
					CarWashState.availableSlowMachines--;
					SSeq.sortEvents(firstInLine);
					CWS.observable(this);*/
				}
			}
		}
	}
	public double getTime(){
		return time;
	}
	public String getEventType(){
		return eventType;
	}
	public Car getCar(){
		return car;
	}
}