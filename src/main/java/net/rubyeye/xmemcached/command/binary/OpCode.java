package net.rubyeye.xmemcached.command.binary;

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
	REPLACE {
		@Override
		public byte fieldValue() {
			return 0x03;

		}
	},
	ADD {
		@Override
		public byte fieldValue() {
			return 0x02;

		}
	},
	APPEND {
		@Override
		public byte fieldValue() {
			return 0x0E;

		}
	},
	PREPEND {
		@Override
		public byte fieldValue() {
			return 0x0F;

		}
	},
	DELETE {
		@Override
		public byte fieldValue() {
			return 0x04;

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
	};
	public abstract byte fieldValue();
}
