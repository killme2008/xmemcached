package com.google.code.yanf4j.core;

/**
 * 
 * 
 * Controller״̬������
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ����05:59:44
 */
public interface ControllerStateListener {

	/**
	 * ������ʱ����
	 * 
	 * @param controller
	 */
	public void onStarted(final Controller controller);

	/**
	 * ����ʱ����
	 * 
	 * @param controller
	 */
	public void onReady(final Controller controller);

	/**
	 * �������ӱ��رյ�ʱ����ã��������������ȷ���벻Ҫ�����˷�������ȷ�ؿ���
	 * 
	 * @param controller
	 */
	public void onAllSessionClosed(final Controller controller);

	/**
	 * ���ر�ʱ����
	 * @param controller
	 */
	public void onStopped(final Controller controller);

	/**
	 * �����쳣ʱ����
	 * @param controller
	 * @param t
	 */
	public void onException(final Controller controller, Throwable t);
}
