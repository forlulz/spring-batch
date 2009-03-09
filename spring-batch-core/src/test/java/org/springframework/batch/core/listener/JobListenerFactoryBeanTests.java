/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.listener;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;

/**
 * @author Lucas Ward
 * 
 */
public class JobListenerFactoryBeanTests {

	JobListenerFactoryBean factoryBean;

	@Before
	public void setUp() {
		factoryBean = new JobListenerFactoryBean();
	}

	@Test
	public void testWithInterface() throws Exception {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		factoryBean.setDelegate(delegate);
		JobExecutionListener listener = (JobExecutionListener) factoryBean.getObject();
		JobExecution jobExecution = new JobExecution(11L);
		listener.beforeJob(jobExecution);
		listener.afterJob(jobExecution);
		assertTrue(delegate.beforeJobCalled);
		assertTrue(delegate.afterJobCalled);
	}

	@Test
	public void testWithAnnotations() throws Exception {
		AnnotatedTestClass delegate = new AnnotatedTestClass();
		factoryBean.setDelegate(delegate);
		JobExecutionListener listener = (JobExecutionListener) factoryBean.getObject();
		JobExecution jobExecution = new JobExecution(11L);
		listener.beforeJob(jobExecution);
		listener.afterJob(jobExecution);
		assertTrue(delegate.beforeJobCalled);
		assertTrue(delegate.afterJobCalled);
	}

	@Test
	public void testFactoryMethod() throws Exception {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		Object listener = JobListenerFactoryBean.getListener(delegate);
		assertTrue(listener instanceof JobExecutionListener);
		((JobExecutionListener) listener).afterJob(new JobExecution(11L));
		assertTrue(delegate.afterJobCalled);
	}

	@Test
	public void testUseInHashSet() throws Exception {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		Object listener = JobListenerFactoryBean.getListener(delegate);
		Object other = JobListenerFactoryBean.getListener(delegate);
		assertTrue(listener instanceof JobExecutionListener);
		Set<JobExecutionListener> listeners = new HashSet<JobExecutionListener>();
		listeners.add((JobExecutionListener) listener);
		listeners.add((JobExecutionListener) other);
		assertTrue(listeners.contains(listener));
		assertEquals(1, listeners.size());
	}

	@Test
	public void testAnnotationsIsListener() throws Exception {
		assertTrue(JobListenerFactoryBean.isListener(new Object() {
			@SuppressWarnings("unused")
			@BeforeJob
			public void foo(JobExecution execution) {
			}
		}));
	}

	@Test
	public void testInterfaceIsListener() throws Exception {
		assertTrue(JobListenerFactoryBean.isListener(new JobListenerWithInterface()));
	}

	@Test
	public void testEqualityOfProxies() throws Exception {
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		Object listener1 = JobListenerFactoryBean.getListener(delegate);
		Object listener2 = JobListenerFactoryBean.getListener(delegate);
		assertEquals(listener1, listener2);
	}

	private class JobListenerWithInterface implements JobExecutionListener {

		boolean beforeJobCalled = false;

		boolean afterJobCalled = false;

		public void afterJob(JobExecution jobExecution) {
			afterJobCalled = true;
		}

		public void beforeJob(JobExecution jobExecution) {
			beforeJobCalled = true;
		}

	}

	private class AnnotatedTestClass {

		boolean beforeJobCalled = false;

		boolean afterJobCalled = false;

		@BeforeJob
		public void before() {
			beforeJobCalled = true;
		}

		@AfterJob
		public void after() {
			afterJobCalled = true;
		}
	}
}