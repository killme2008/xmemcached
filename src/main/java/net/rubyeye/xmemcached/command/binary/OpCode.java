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
	};
	public abstract byte fieldValue();
}
