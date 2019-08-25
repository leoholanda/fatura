package br.com.aee.jobs;

import javax.inject.Inject;

import org.apache.deltaspike.scheduler.api.Scheduled;

import br.com.aee.controller.FaturaBean;

@Scheduled(cronExpression = "0 0/1 * * * ?", onStartup = false)
//@Scheduled(cronExpression = "0 10 12 15 * ?")
public class GeraFaturaJob implements Runnable {

	@Inject
	private FaturaBean faturaBean;

//	@Override
//	public void execute(JobExecutionContext context) throws JobExecutionException {
//		System.out.println(">>>> execute");
//		faturaBean.geraFatura();
//	}

	@Override
	public void run() {
		System.out.println(">>>> execute");
	}

}
