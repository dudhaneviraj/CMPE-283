
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.LocalizableMessage;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class Navigator {

	private static void navigate(ServiceInstance si, Folder rootFolder, String vmFolderPath) throws InvalidProperty, RuntimeFault, RemoteException, InterruptedException
	{
		ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("Datacenter");
		
		String[] path = vmFolderPath.split("/");
		int revcounter = path.length;
		int revcounterer =0;
		
			for(int i =0;i<mes.length;i++)
			{
				Datacenter dataCenter = (Datacenter) mes[i];
				Folder vmFolder = dataCenter.getVmFolder();
				if(revcounter>0)
				{
						revcounter--;
					revcounterer++;
					while(revcounter>0) 
					{
						ManagedEntity[] allVms = vmFolder.getChildEntity();
						for(ManagedEntity entity: allVms)
						{
							Folder folder = (Folder)entity;
							if(folder.getName().equals(path[revcounterer]))
							{
								vmFolder = folder;
								revcounterer++;
								revcounter--;
								break;
							}
						}
					}
					getHostDetails(rootFolder);
					for(ManagedEntity entity: vmFolder.getChildEntity())
					{
						VirtualMachine vm = (VirtualMachine)entity;
						GetVMDetails(si, rootFolder, vm.getName());
					}
				}
				else
				{
					
					ManagedEntity[] allVms = vmFolder.getChildEntity();
					getHostDetails(rootFolder);
					for(ManagedEntity entity: allVms)
					{
						VirtualMachine vm = (VirtualMachine)entity;
						
						GetVMDetails(si, rootFolder, vm.getName());
					}
				}
				
			}
		
	}
	private static void createSnapshot(VirtualMachine vm)
	{
	
		try 
		{
			Task t = vm.createSnapshot_Task("Snapshot Of "+vm.getName()+"For Extra Credit", vm.getName()+"Snapshot", false, true);
			System.out.println("Task Result: "+ t.waitForTask());
			showTask(vm);
			
		} 
		catch (Exception e) {
			
			System.out.println(e);
		} 
		
		
	}
	private static void clone(VirtualMachine vm)
	{
		  VirtualMachineCloneSpec spec = new VirtualMachineCloneSpec();
          VirtualMachineRelocateSpec vmrs = new VirtualMachineRelocateSpec();
          
          spec.setPowerOn(false);
          spec.setTemplate(false);
          spec.setLocation(vmrs);

          try {
        	  System.out.println("*********************Cloning"+vm.getName()+"***************************");
                Folder parent = (Folder) vm.getParent();
                Task task = vm.cloneVM_Task(parent, vm.getName()+"-clone", spec);
            	System.out.println("Task Result: "+task.waitForTask());
				if(task.getTaskInfo().getState()==TaskInfoState.error)
				{
					System.out.println("Error Local Message:"+task.getTaskInfo().getError().getLocalizedMessage());
				}
                showTask(vm);
          } catch (Exception e)
          {
                System.out.println( e);
          }
	}
	private static void showTask(VirtualMachine vm) throws InvalidProperty, RuntimeFault, RemoteException
	{
		
		System.out.println("***************Tasks for VM: " + vm.getName()+"********************");
		Task[] tasks = vm.getRecentTasks();
		for(Task t : tasks)
		{
			System.out.println("***************************************************************");
			System.out.println("Task Name:  " + t.getTaskInfo().getName());
			System.out.println("Task Result:  " + t.getTaskInfo().getState());
			System.out.println("Target:  " + t.getTaskInfo().getEntityName());
			System.out.println("Start Time: "+t.getTaskInfo().getStartTime().getTime());
			System.out.println("Complete Time:  " + t.getTaskInfo().getCompleteTime().getTime());
			if(t.getTaskInfo().getState()==TaskInfoState.error)
			{	
				System.out.println("Localized Error Message: " + t.getTaskInfo().getError().localizedMessage);
			}
			System.out.println("***************************************************************");
		}
	}
	
	private static void migrate(ServiceInstance si, Folder rootFolder,VirtualMachine vm) throws InvalidProperty, RuntimeFault, RemoteException, InterruptedException
	{
		//get hosts
		boolean machineMigrated = false;
		ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
		if(mes.length<1)
		{
				System.out.println("Migration Failed!\nOnly One Host!");
				return;
		}
			// hosts>1
			HostSystem destination =null;
			for(int i =0; i<mes.length;i++)
			{
				HostSystem ds = (HostSystem)mes[i];
				VirtualMachine[] vms = ds.getVms();
				for(VirtualMachine tempVm: vms)
				{
					if(tempVm.getName().equals(vm.getName()))
					{
						System.out.println("Current VM Host: "+ ds.getName());

						//check compatibility of other destinations
						destination = selectDestination(si,rootFolder,vm,ds);
						System.out.println("Migrating VM on host: " + destination.getName());
						
						Task task = vm.migrateVM_Task(null, destination,VirtualMachineMovePriority.highPriority, 
							       vm.getRuntime().getPowerState());
						System.out.println("Task Result: "+task.waitForTask());
						if(task.getTaskInfo().getState()==TaskInfoState.error)
						{
							System.out.println("Error Localized Message:"+task.getTaskInfo().getError().getLocalizedMessage());
						}
						showTask(vm);
						return;		       
								 
					}
				}
				if(machineMigrated)
				{
					return;
				}
				
			}
		
		
	
	}

		public static void getHostDetails(Folder rootFolder) throws InvalidProperty, RuntimeFault, RemoteException
	{
		int cnt = 0;
			
			ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
			
			for(ManagedEntity temp: mes)
			{
				HostSystem ds = (HostSystem)temp;
				System.out.println("*********************Host Information**************************");
				System.out.println("host[" + cnt++ +"]");
				System.out.println("Host Name: "+ds.getName());
				System.out.println("Product Full Name: " + ds.getConfig().getProduct().getFullName());
				System.out.println("***************************************************************");
			}
		
		
	}
	public static void GetVMDetails(ServiceInstance si, Folder rootFolder, String vmName) throws InvalidProperty, RuntimeFault, RemoteException, InterruptedException
	{
		boolean vmFound = false;
		ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");	
		for(int i =0; i< mes.length;i++)
		{
			
			VirtualMachine vm = (VirtualMachine)mes[i];
			if(vm.getName().equalsIgnoreCase(vmName))
			{
				System.out.println("*************************"+vm.getName()+"************************");
				System.out.println("Virtual Machine Name:  "+vm.getName());
				System.out.println("VM Guest OS Full Name: "+ vm.getConfig().getGuestFullName());
				System.out.println("VM Guest State: "+ vm.getGuest().getGuestState());
				System.out.println("VM Power State:"+vm.getRuntime().getPowerState());
				System.out.println("ESXi Host: "+vm.getSummary().getRuntime().getHost().get_value());
				System.out.println("***************************************************************\n");
				vmFound = true;	
			
				createSnapshot(vm);
			
				clone(vm);
			
				migrate(si,rootFolder, vm);
			
				showTask(vm);
				break;
			}
					
		}
		
		if(!vmFound)
		{
			System.out.println("Virtual Machine with the name: " + vmName + " not found");
		}
	}
	
	private static HostSystem selectDestination(ServiceInstance si,Folder rootFolder, VirtualMachine vm, HostSystem ds) throws InvalidProperty, RuntimeFault, RemoteException 
	{
		ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
		for(int i =0; i<mes.length;)
		{
			HostSystem host = (HostSystem)mes[i];
			if(host.getName().equals(ds.getName()))
			{
				i++;
			}
			else
			{
				return host;
			}		
		}
		return null;
	}
	
	public static void main(String[] args) {
		ServiceInstance si = null;

		try 
		{
			si = new ServiceInstance(new URL(args[0]),args[1],args[2],true);
			Folder rootFolder = si.getRootFolder();
			
			navigate(si,rootFolder,args[3]);

		}
		catch (Exception e) {
			// TODO Auto*generated catch block
			e.printStackTrace();
		}

	}

}
