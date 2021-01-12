import React from 'react';
import {
  ListView,
  View,
  StyleSheet,
  Text,
} from '@hippy/react';

const STYLE_LOADING = 100;
const MAX_FETCH_TIMES = 100;
const mockDataArray = [
  { style: 1 },
  { style: 2 },
  { style: 5 },
  { style: 1 },
  { style: 2 },
  { style: 5 },
  { style: 1 },
  { style: 2 },
  { style: 5 },
  { style: 1 },
  { style: 2 },
  { style: 5 },
  { style: 1 },
  { style: 2 },
  { style: 5 },
  { style: 1 },
  { style: 2 },
  { style: 5 },
  { style: 1 },
  { style: 2 },
  { style: 5 },
  { style: 1 },
  { style: 2 },
  { style: 5 },
  { style: 1 },
  { style: 2 },
  { style: 5 },
  { style: 1 },
  { style: 2 },
  { style: 5 },
];

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#fff',
    collapsable: false,
  },
  itemContainer: {
    padding: 12,
  },
  separatorLine: {
    marginLeft: 12,
    marginRight: 12,
    height: 0.5,
    backgroundColor: '#e5e5e5',
  },
  loading: {
    fontSize: 11,
    color: '#aaaaaa',
    alignSelf: 'center',
  },
  footerContainer: {
    backgroundColor: 'green',
  },
});


function Style1({ index }) {
  return (
    <View style={styles.container}>
      <Text numberOfLines={1}>{ `${index}: Style 1 UI` }</Text>
    </View>
  );
}

function Style2({ index }) {
  return (
    <View style={styles.container}>
      <Text numberOfLines={1}>{ `${index}: Style 2 UI` }</Text>
    </View>
  );
}

function Style5({ index }) {
  return (
    <View style={styles.container}>
      <Text numberOfLines={1}>{ `${index}: Style 5 UI` }</Text>
    </View>
  );
}

export default class ListExample extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      dataSource: mockDataArray,
      fetchingDataFlag: false,
      footerLoadingText: '正在加载...',
    };
    this.fetchTimes = 0;
    this.mockFetchData = this.mockFetchData.bind(this);
    this.getRenderRow = this.getRenderRow.bind(this);
    this.onEndReached = this.onEndReached.bind(this);
    this.getRowType = this.getRowType.bind(this);
    this.getRowKey = this.getRowKey.bind(this);
    this.renderPullFooter = this.renderPullFooter.bind(this);
  }

  /**
   * 渲染 footer 组件
   */
  renderPullFooter() {
    const { footerLoadingText } = this.state;
    return (
      <View style={styles.container}>
        <Text style={styles.loading}>Loading now...</Text>
      </View>
    );
  }

  async onEndReached() {
    const { dataSource, fetchingDataFlag } = this.state;
    // ensure that only one fetching task would be running
    if (fetchingDataFlag) return;
    // this.setState({
    //   fetchingDataFlag: true,
    //   dataSource: dataSource.concat([{ style: STYLE_LOADING }]),
    // });
    const newData = await this.mockFetchData();
    // const lastLineItem = dataSource[dataSource.length - 1];
    // if (lastLineItem && lastLineItem.style === STYLE_LOADING) {
    //   dataSource.pop();
    // }
    const newDataSource = dataSource.concat(newData);
    this.setState({ dataSource: newDataSource });
  }

  // TODO android onAppear不完善，暂时不适用
  // 曝光
  // eslint-disable-next-line class-methods-use-this
  onAppear(index) {
    // eslint-disable-next-line no-console
    console.log('onAppear', index);
  }

  // TODO android onDisappear不完善，暂时不适用
  // 隐藏
  // eslint-disable-next-line class-methods-use-this
  onDisappear(index) {
    // eslint-disable-next-line no-console
    console.log('onDisappear', index);
  }

  getRowType(index) {
    if(this.isStickyItem(index)){ 
           return 99;
    }
    const self = this;
    const item = self.state.dataSource[index];
    return item.style;
  }

  /* eslint-disable-next-line class-methods-use-this */
  getRowKey(index) {
    return `row-${index}`;
  }

  getRenderRow(index) {
    const { dataSource } = this.state;
    let styleUI = null;
    const rowData = dataSource[index];
    const isLastItem = dataSource.length === index + 1;
    switch (rowData.style) {
      case 1:
        styleUI = <Style1 index={index} />;
        break;
      case 2:
        styleUI = <Style2 index={index} />;
        break;
      case 5:
        styleUI = <Style5 index={index} />;
        break;
      case STYLE_LOADING:
        styleUI = <Text style={styles.loading}>Loading now...</Text>;
        break;
      default:
        // pass
    }
    return (
      <View style={styles.container}>
        <View style={styles.itemContainer}>
          {styleUI}
        </View>
        {!isLastItem ? <View style={styles.separatorLine} /> : null }
      </View>
    );
  }

  mockFetchData() {
    return new Promise((resolve) => {
      setTimeout(() => {
        this.setState({
          fetchingDataFlag: false,
        });
        this.fetchTimes += 1;
        if (this.fetchTimes >= MAX_FETCH_TIMES) {
          return resolve([]);
        }
        return resolve(mockDataArray);
      }, 1000);
    });
  }
  isStickyItem(index) {
    return index%10 ===0;
  }
  render() {
    const { dataSource } = this.state;
    return (
      <ListView
        style={{ flex: 1, backgroundColor: '#ffffff' }}
        numberOfRows={dataSource.length}
        renderRow={this.getRenderRow}
        onEndReached={this.onEndReached}
        getRowType={this.getRowType}
        getRowKey={this.getRowKey}
        renderPullFooter={this.renderPullFooter}
        initialListSize={500}
        rowShouldSticky={index =>this.isStickyItem(index)}
        onAppear={index => this.onAppear(index)}
        onDisappear={index => this.onDisappear(index)}
      />
    );
  }
}
