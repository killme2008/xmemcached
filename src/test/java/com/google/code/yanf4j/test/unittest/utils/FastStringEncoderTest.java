package com.google.code.yanf4j.test.unittest.utils;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;

import net.rubyeye.xmemcached.utils.ByteUtils;
import net.rubyeye.xmemcached.utils.FastStringEncoder;

public class FastStringEncoderTest {

	@Test
	public void testASCII() {
		this.assertEncodeEquals("hello world!@#$%^&*()_+|");
		for (int i = 0; i < 10000; i++) {
			String uuid = UUID.randomUUID().toString();
			this.assertEncodeEquals(uuid);
		}
	}

	private void assertEncodeEquals(String s) {
		assertEquals(s, ByteUtils.getString(FastStringEncoder.encodeUTF8(s)));
	}

	@Test
	public void testCJK() {
		this.assertEncodeEquals("中华人民共和国");
		this.assertEncodeEquals("我能吞下玻璃而不傷身體");
		this.assertEncodeEquals("驚いた彼は道を走っていった。");
		this.assertEncodeEquals(" 나는 유리를 먹을 수 있어요. 그래도 아프지 않아요");
		this.assertEncodeEquals("私はガラスを食べられます。それは私を傷つけません");
		this.assertEncodeEquals("ฉันกินกระจกได้ แต่มันไม่ทำให้ฉันเจ็บ");
	}

	@Test
	public void testBigString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 65535; i++) {
			sb.append("a");
		}
		this.assertEncodeEquals(sb.toString());
	}

	@Test
	public void testEmoj() {
		this.assertEncodeEquals(
				"😀 😃 😄 😁 😆 😅 😂 🤣 ☺️ 😊 😇 🙂 🙃 😉 😌 😍 😘 😗 😙 😚 😋 😜 😝 😛 🤑 🤗 🤓 😎 🤡 🤠 😏 😒 😞 😔 😟 😕 🙁 ☹️ 😣 😖 😫 😩 😤 😠 😡 😶 😐 😑 😯 😦 😧 😮 😲 😵 😳 😱 😨 😰 😢 😥 🤤 😭 😓 😪 😴 🙄 🤔 🤥 😬 🤐 🤢 🤧 😷 🤒 🤕 😈 👿 👹 👺 💩 👻 💀 ☠️ 👽 👾 🤖 🎃 😺 😸 😹 😻 😼 😽 🙀 😿 😾 ");
	}
}
