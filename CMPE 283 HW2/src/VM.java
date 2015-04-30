import java.net.URL;
import java.util.*;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
public class VM
{
	public void enumerateAllHosts(String arg0, String arg1, String arg2)
	{
		try
		{
			ServiceInstance si = new ServiceInstance(new URL("https://"+arg0+"/sdk"), arg1,	arg2, true);
			Folder rootFolder = si.getRootFolder();
			ManagedEntity[] hostmanagedEntities = new InventoryNavigator(
			si.getRootFolder()).searchManagedEntities("HostSystem");
			for(ManagedEntity mi:hostmanagedEntities)
			{
				
				HostSystem hs=(HostSystem)mi;
				System.out.println("***********************************************************");		
				System.out.println("Host Name: "+hs.getName());
				System.out.println("Product Full Name: "+si.getAboutInfo().getFullName());
				System.out.println("***********************************************************");		
				
			}	
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}
	public void extraCredit(String arg0, String arg1, String arg2,String arg3)
	{
		
	}
	public void enumerateTasks(String arg0, String arg1, String arg2,String arg3)
	{
		try
		{

			ServiceInstance si = new ServiceInstance(new URL("https://"+arg0+"/sdk"), arg1,	arg2, true);
			ManagedEntity [] mes = new
					InventoryNavigator(si.getRootFolder()). searchManagedEntities("VirtualMachine");
			if (mes == null || mes.length == 0) {
				return;
			}
			for (ManagedEntity me: mes) 
			{	
				VirtualMachine vm = (VirtualMachine) me;
				if(vm.getName().equals(arg3))
				{
					Task[] tarray=vm.getRecentTasks();
					for(Task temp:tarray)
					{
							
							System.out.println("*******************"+temp.getTaskInfo().getName()+"******************");
							System.out.println("Name Of Operation: "+temp.getTaskInfo().getName());
							System.out.println("Task Target:"+temp.getTaskInfo().entityName);
							
							System.out.println("Task Start Time: "+temp.getTaskInfo().getStartTime().getTime());
							System.out.println("Task End Time: "+temp.getTaskInfo().getCompleteTime().getTime());
							System.out.println("Task Result: "+temp.getTaskInfo().getState());
							if(temp.getTaskInfo().getState()==TaskInfoState.error)
							{
								System.out.println("Localized Error Message:"+temp.getTaskInfo().getError().getLocalizedMessage());
							}
							System.out.println("***********************************************************");		
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}
	public void enumerateAllVMS(String arg0, String arg1, String arg2,String arg3)
	{
		try
		{
			ServiceInstance si = new ServiceInstance(new URL("https://"+arg0+"/sdk"), arg1,	arg2, true);
			Folder rootFolder = si.getRootFolder();
			String name = rootFolder.getName();
			ManagedEntity[] hostmanagedEntities = new InventoryNavigator(
					si.getRootFolder()).searchManagedEntities("HostSystem");
			ManagedEntity [] mes = new
					InventoryNavigator(si.getRootFolder()). searchManagedEntities("VirtualMachine");
			if (mes == null || mes.length == 0) {
				return;
			}
			for (ManagedEntity me: mes) 
			{	
				VirtualMachine vm = (VirtualMachine) me;
				if(vm.getName().equals(arg3))
				{
					VirtualMachineConfigInfo vminfo = vm.getConfig();
					VirtualMachineCapability vmc = vm.getCapability();	
					System.out.println("************************"+vm.getName()+"***********************");
					System.out.println("VM Name " + vm.getName());  
					System.out.println("ESXi Host:"+vm.getSummary().getRuntime().host.get_value());
					System.out.println("Guest OS Full Name:"+vminfo.getGuestFullName());
					System.out.println("Guest State: "+vm.getGuest().guestState);
					System.out.println("Power State: "+vm.getRuntime().powerState);
					System.out.println("***********************************************************");		
					System.out.println("************************Snapshot***************************");
					Task t = vm.createSnapshot_Task("Snapshot of "+vm.getName(),"SnapShot",true, true);
					System.out.println("Snapshot Result: " + t.waitForTask());
					if(t.getTaskInfo().getState()==TaskInfoState.error)
					{
						System.out.println("Localized Error Message: "+t.getTaskInfo().getError().getLocalizedMessage());
					}
					System.out.println("***********************************************************");
					System.out.println("************************Cloning****************************");
					VirtualMachineCloneSpec clone = new VirtualMachineCloneSpec();
					clone.setLocation(new VirtualMachineRelocateSpec());
					clone.setPowerOn(false);
					clone.setTemplate(false);
					Task tsk=vm.cloneVM_Task((Folder)vm.getParent(),vm.getName()+"-clone",clone);
					System.out.println("Cloning result:"+tsk.waitForTask());
					if(tsk.getTaskInfo().getState()==TaskInfoState.error)
					{
						System.out.println("Localized Error Message: "+tsk.getTaskInfo().getError().getLocalizedMessage());
					}
					System.out.println("***********************************************************");					
					System.out.println("*************************Migration*************************");
					if(hostmanagedEntities.length<=1)
					{
						System.out.println("Only One Host Is Present!");
						return;
					}
					ManagedEntity m=null;
					int flag=0;
					for(ManagedEntity mi:hostmanagedEntities)
					{
						HostSystem hs=(HostSystem)mi;
						Datastore[] ds=hs.getDatastores();
						for(Datastore d:ds)
						{
							VirtualMachine[] vms=d.getVms();
							for(ManagedEntity v:vms)
							{
								if(v.getName().equalsIgnoreCase(arg3))
								{
									if(flag==0)
									{	
										System.out.println("VM was Found On Host: "+hs.getName());

										m=mi;
									    flag=1;
									}
									break;
								}
							}
						}		
					}	
					for(ManagedEntity mitemp:hostmanagedEntities)
					{
						HostSystem hs=(HostSystem)mitemp;
						if(mitemp.equals(m))
						{
							continue;
						}
						Task tmove=vm.migrateVM_Task(vm.getResourcePool(), hs,VirtualMachineMovePriority.highPriority,vm.getRuntime().powerState);
						System.out.println("Migration Result:"+tmove.waitForTask());
						if(tmove.getTaskInfo().getState()==TaskInfoState.error)
						{
							System.out.println("Error Local Message:"+tmove.getTaskInfo().getError().getLocalizedMessage());
						}
						System.out.println("VM Migrated On Host: "+hs.getName());
						System.out.println("***********************************************************");					
						return;
					}
				}
			}	
			
			
			si.getServerConnection().logout();

		}
		catch(Exception e)
		{
			System.out.print(e);
		}
	}
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception
	{
		int choice=0;
		Scanner s=new Scanner(System.in);
		
		VM a=new VM();
		while(true)
		{
			System.out.println("1.Enumerate All Hosts");
			System.out.println("2. Enumerate All VMs");
			System.out.println("Enter Your Choice:");
		    choice=Integer.parseInt(s.nextLine());
			switch(choice)
			{
				case 1:
						a.enumerateAllHosts(args[0], args[1], args[2]);
						break;
				case 2:
						a.enumerateAllVMS(args[0], args[1], args[2],args[3]);
						a.enumerateTasks(args[0], args[1], args[2],args[3]);
						break;
				case 3:
						a.extraCredit(args[0], args[1], args[2],args[3]);
						break;
				case 4: 
						System.exit(0);
				default:
						System.out.println("Incorrect Choice!");
			}
			
		}
	}
}



































