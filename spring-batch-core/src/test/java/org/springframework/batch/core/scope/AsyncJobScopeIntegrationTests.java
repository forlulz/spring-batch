package org.springframework.batch.core.scope;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.scope.context.JobContext;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncJobScopeIntegrationTests implements BeanFactoryAware {

	private Log logger = LogFactory.getLog(getClass());

	@Autowired
	@Qualifier("simple")
	private Collaborator simple;

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private ListableBeanFactory beanFactory;

	private int beanCount;

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Before
	public void countBeans() {
		JobSynchronizationManager.release();
		beanCount = beanFactory.getBeanDefinitionCount();
	}

	@After
	public void cleanUp() {
		JobSynchronizationManager.close();
		// Check that all temporary bean definitions are cleaned up
		assertEquals(beanCount, beanFactory.getBeanDefinitionCount());
	}

	@Test
	public void testSimpleProperty() throws Exception {
		JobExecution jobExecution = new JobExecution(11L);
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		executionContext.put("foo", "bar");
		JobSynchronizationManager.register(jobExecution);
		assertEquals("bar", simple.getName());
	}

	@Test
	public void testGetMultipleInMultipleThreads() throws Exception {

		List<FutureTask<String>> tasks = new ArrayList<FutureTask<String>>();

		for (int i = 0; i < 12; i++) {
			final String value = "foo" + i;
			final Long id = 123L + i;
			FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
				public String call() throws Exception {
					JobExecution jobExecution = new JobExecution(id);
					ExecutionContext executionContext = jobExecution.getExecutionContext();
					executionContext.put("foo", value);
					JobContext context = JobSynchronizationManager.register(jobExecution);
					logger.debug("Registered: " + context.getJobExecutionContext());
					try {
						return simple.getName();
					}
					finally {
						JobSynchronizationManager.close();
					}
				}
			});
			tasks.add(task);
			taskExecutor.execute(task);
		}

		int i = 0;
		for (FutureTask<String> task : tasks) {
			assertEquals("foo" + i, task.get());
			i++;
		}

	}

	@Test
	public void testGetSameInMultipleThreads() throws Exception {

		List<FutureTask<String>> tasks = new ArrayList<FutureTask<String>>();
		final JobExecution jobExecution = new JobExecution(11L);
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		executionContext.put("foo", "foo");
		JobSynchronizationManager.register(jobExecution);
		assertEquals("foo", simple.getName());

		for (int i = 0; i < 12; i++) {
			final String value = "foo" + i;
			FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
				public String call() throws Exception {
					ExecutionContext executionContext = jobExecution.getExecutionContext();
					executionContext.put("foo", value);
					JobContext context = JobSynchronizationManager.register(jobExecution);
					logger.debug("Registered: " + context.getJobExecutionContext());
					try {
						return simple.getName();
					}
					finally {
						JobSynchronizationManager.close();
					}
				}
			});
			tasks.add(task);
			taskExecutor.execute(task);
		}

		int i = 0;
		for (FutureTask<String> task : tasks) {
			assertEquals("foo", task.get());
			i++;
		}

		// Don't close the outer scope until all tasks are finished. This should
		// always be the case if using an AbstractJob
		JobSynchronizationManager.close();

	}

}