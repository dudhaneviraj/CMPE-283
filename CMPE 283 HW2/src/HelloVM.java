import java.net.URL;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
public class HelloVM
{
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception
	{
		
		
		try
		{
			
				args[0]="192.168.16.129";
				args[1]="root";
				args[2]="1234567";
			  //args[0]  IP Address
			  //args[1]  Username
			  //args[2]  Password
			
		ServiceInstance si = new ServiceInstance(new URL("https://"+args[0]+"/sdk"), args[1],	args[2], true);
		Folder rootFolder = si.getRootFolder();
	
		String name = rootFolder.getName();
		System.out.println("root:" + name);
				ManagedEntity[] hostmanagedEntities = new InventoryNavigator(
				si.getRootFolder()).searchManagedEntities("HostSystem");
		
		for(ManagedEntity mi:hostmanagedEntities)
		{
			HostSystem hs=(HostSystem)mi;
			System.out.println("Host Name"+hs.getName());
			System.out.println("Product Full Name: "+si.getAboutInfo().getFullName());
			Datastore[] ds=hs.getDatastores();
			for(Datastore d:ds)
			{
				System.out.println("Datastore Name: "+d.getName().trim()+"\t Capacity: "+d.getSummary().capacity+"\t\t FreeSpace: "+d.getSummary().freeSpace);
			}
			Network[] nw=hs.getNetworks();
			for(Network n:nw)
			{
				System.out.println("Network: "+n.getName());
			}
		}
			
		ManagedEntity [] mes = new
				InventoryNavigator(rootFolder). searchManagedEntities("VirtualMachine");
		if (mes == null || mes.length == 0) {
			return;
		}		
		for (ManagedEntity me: mes) 
		{
			
			VirtualMachine vm = (VirtualMachine) me;
			VirtualMachineConfigInfo vminfo = vm.getConfig();
			VirtualMachineCapability vmc = vm.getCapability();	
			System.out.println("************************"+vm.getName()+"***********************");
			System.out.println("VM Name " + vm.getName());
			System.out.println("Guest OS Full Name:"+vminfo.getGuestFullName());
			System.out.println("Guest State State: "+vm.getGuest().guestState);
			System.out.println("Power State: "+vm.getRuntime().powerState);

			
			String str=""+vm.getRuntime().powerState;
			str=str.trim();
			VirtualMachine vm1 = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vm.getName());
			if(str.equalsIgnoreCase("poweredOff"))
			{
				Task task = vm1.powerOnVM_Task(null);
				if(task.waitForMe()==Task.SUCCESS)
				{
					System.out.println("Task Name:"+task.getTaskInfo().name);
					System.out.println("Task Start Time:"+task.getTaskInfo().startTime.getTime());
					System.out.println(vm.getName() + " : Is Now Powered On");
					System.out.println("Completion Time:"+task.getTaskInfo().completeTime.getTime());
				}
			}
			else if(str.equalsIgnoreCase("poweredOn"))
 			{
				Task task = vm1.powerOffVM_Task();
				if(task.waitForMe()==Task.SUCCESS)
				{
					System.out.println("Task Name:"+task.getTaskInfo().name);
					System.out.println("Task Start Time:"+task.getTaskInfo().startTime.getTime());			
					System.out.println(vm.getName() + " : Is Now Powered Off");
					System.out.println("Completion Time:"+task.getTaskInfo().completeTime.getTime());		
				}
 			}
		}
		System.out.println("***********************************************************");
		si.getServerConnection().logout();
		}
		catch(Exception e){
			System.out.println("Exception:"+e);
		}
	}
}