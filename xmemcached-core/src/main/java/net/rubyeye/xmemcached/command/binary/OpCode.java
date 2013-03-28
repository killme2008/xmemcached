/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.command.binary;

/**
 * Binary command Opcodes
 * 
 * @author dennis
 * 
 */
public enum OpCode {
	GET {
		@Override
		public byte fieldValue() {
			return 0x00;

		}
	},
	GET_QUIETLY {
		@Override
		public byte fieldValue() {
			return 0x09;

		}
	},
	GET_KEY {
		@Override
		public byte fieldValue() {
			return 0x0C;

		}
	},
	GET_KEY_QUIETLY {
		@Override
		public byte fieldValue() {
			return 0x0D;

		}
	},
	SET {
		@Override
		public byte fieldValue() {
			return 0x01;

		}
	},
	SET_QUIETLY {
		@Override
		public byte fieldValue() {
			return 0x11;

		}
	},
	REPLACE {
		@Override
		public byte fieldValue() {
			return 0x03;

		}
	},
	REPLACE_QUIETLY {
		@Override
		public byte fieldValue() {
			return 0x13;

		}
	},
	ADD {
		@Override
		public byte fieldValue() {
			return 0x02;

		}
	},
	ADD_QUIETLY {
		@Override
		public byte fieldValue() {
			return 0x12;

		}
	},
	APPEND {
		@Override
		public byte fieldValue() {
			return 0x0E;

		}
	},
	APPEND_QUIETLY {
		@Override
		public byte fieldValue() {
			return 0x19;

		}
	},
	PREPEND {
		@Override
		public byte fieldValue() {
			return 0x0F;

		}
	},
	PREPEND_QUIETLY {
		@Override
		public byte fieldValue() {
			return 0x1A;

		}
	},
	DELETE {
		@Override
		public byte fieldValue() {
			return 0x04;

		}
	},
	DELETE_QUIETLY {
		@Override
		public byte fieldValue() {
			return 0x14;

		}
	},
	VERSION {
		@Override
		public byte fieldValue() {
			return 0x0b;

		}
	},
	QUITQ {
		@Override
		public byte fieldValue() {
			return 0x17;

		}
	},
	STAT {
		@Override
		public byte fieldValue() {
			return 0x10;

		}
	},
	NOOP {
		@Override
		public byte fieldValue() {
			return 0x0a;

		}
	},
	INCREMENT {
		@Override
		public byte fieldValue() {
			return 0x05;

		}
	},
	INCREMENT_QUIETLY {
		@Override
		public byte fieldValue() {
			return 0x15;

		}
	},
	DECREMENT {
		@Override
		public byte fieldValue() {
			return 0x06;

		}
	},
	DECREMENT_QUIETLY {
		@Override
		public byte fieldValue() {
			return 0x16;

		}
	},
	FLUSH {
		@Override
		public byte fieldValue() {
			return 0x08;

		}
	},
	FLUSH_QUIETLY {
		@Override
		public byte fieldValue() {
			return 0x18;

		}
	},
	AUTH_LIST_MECHANISMS {
		@Override
		public byte fieldValue() {
			return 0x20;

		}
	},
	AUTH_START {
		@Override
		public byte fieldValue() {
			return 0x21;

		}
	},
	AUTH_STEP {
		@Override
		public byte fieldValue() {
			return 0x22;

		}
	},

	VERBOSITY {
		@Override
		public byte fieldValue() {
			return 0x1b;

		}
	},

	TOUCH {
		@Override
		public byte fieldValue() {
			return 0x1c;

		}
	},
	GAT {
		@Override
		public byte fieldValue() {
			return 0x1d;

		}
	},
	GATQ {
		@Override
		public byte fieldValue() {
			return 0x1e;

		}
	};

	public abstract byte fieldValue();
}
